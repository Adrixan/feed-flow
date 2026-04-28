package com.prof18.feedflow.feedsync.decsync

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.DecSyncError
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.decsync.library.Decsync
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalStdlibApi::class)
class DecSyncRepository internal constructor(
    private val decSyncSettings: DecSyncSettings,
    private val databaseHelper: DatabaseHelper,
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
) {
    private var decsync: Decsync<Unit>? = null

    fun isDecSyncSetup(): Boolean = decsync != null && decSyncSettings.getDirPath() != null

    fun restoreIfConfigured() {
        val dirPath = decSyncSettings.getDirPath() ?: return
        setup(dirPath).onFailure {
            logger.e("DecSync: failed to restore setup from $dirPath", it)
        }
    }

    fun setup(dirPath: String): Result<Unit> = runCatching {
        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val instance: Decsync<Unit> = DecSyncFactory.create(dir, SYNC_TYPE, null, OWN_APP_ID)
        instance.addListeners()
        instance.initStoredEntries()
        decsync = instance
        decSyncSettings.setDirPath(dirPath)
        logger.d { "DecSync setup complete at: $dirPath" }
    }

    suspend fun syncFeedSources(): SyncResult = withContext(dispatcherProvider.io) {
        val instance = decsync
            ?: return@withContext SyncResult.General(DecSyncError.SyncFeedSourcesFailed)
        val pendingInserts = mutableListOf<PendingFeedInsert>()
        val pendingRemovals = mutableListOf<String>()

        val feedNamesTemp = mutableMapOf<String, String>()
        val feedCategoriesTemp = mutableMapOf<String, String>()

        val subscriptionsListener = EntryUpdateListener<Unit> { _, entry, _ ->
            val feedUrl = entry.key.jsonPrimitive.contentOrNull ?: return@EntryUpdateListener
            val subscribed = entry.value.jsonPrimitive.booleanOrNull ?: return@EntryUpdateListener
            if (subscribed) {
                pendingInserts.add(PendingFeedInsert(feedUrl))
            } else {
                pendingRemovals.add(feedUrl)
            }
        }
        val namesListener = EntryUpdateListener<Unit> { _, entry, _ ->
            val feedUrl = entry.key.jsonPrimitive.contentOrNull ?: return@EntryUpdateListener
            val name = entry.value.jsonPrimitive.contentOrNull ?: return@EntryUpdateListener
            feedNamesTemp[feedUrl] = name
        }
        val categoriesListener = EntryUpdateListener<Unit> { _, entry, _ ->
            val feedUrl = entry.key.jsonPrimitive.contentOrNull ?: return@EntryUpdateListener
            val category = entry.value.jsonPrimitive.contentOrNull ?: return@EntryUpdateListener
            feedCategoriesTemp[feedUrl] = category
        }

        instance.addListenerWithResult(listOf("feeds", "subscriptions"), subscriptionsListener)
        instance.addListenerWithResult(listOf("feeds", "names"), namesListener)
        instance.addListenerWithResult(listOf("feeds", "categories"), categoriesListener)

        runCatching {
            instance.executeStoredEntriesForPathExact(listOf("feeds", "subscriptions"), Unit)
            instance.executeStoredEntriesForPathExact(listOf("feeds", "names"), Unit)
            instance.executeStoredEntriesForPathExact(listOf("feeds", "categories"), Unit)
        }.onFailure {
            logger.e("DecSync feed sources sync failed", it)
            return@withContext SyncResult.General(DecSyncError.SyncFeedSourcesFailed)
        }

        val resolvedInserts = pendingInserts.map { pending ->
            val name = feedNamesTemp[pending.url] ?: pending.url
            val categoryName = feedCategoriesTemp[pending.url]
            ParsedFeedSource.Builder()
                .url(pending.url)
                .title(name)
                .category(categoryName)
                .build()
        }.filterNotNull()

        runCatching {
            if (resolvedInserts.isNotEmpty()) {
                databaseHelper.insertFeedSource(resolvedInserts)
            }
            for (url in pendingRemovals) {
                databaseHelper.deleteFeedSource(url.hashCode().toString())
            }
        }.onFailure {
            logger.e("DecSync: applying feed source changes to DB failed", it)
            return@withContext SyncResult.General(DecSyncError.SyncFeedSourcesFailed)
        }

        pushLocalFeedsToDecSync(instance)

        SyncResult.Success
    }

    suspend fun syncFeedItems(): SyncResult = withContext(dispatcherProvider.io) {
        val instance = decsync
            ?: return@withContext SyncResult.General(DecSyncError.SyncFeedItemsFailed)
        val pendingReadUpdates = mutableListOf<Pair<String, Boolean>>()
        val pendingStarredUpdates = mutableListOf<Pair<String, Boolean>>()

        val readListener = EntryUpdateListener<Unit> { _, entry, _ ->
            val itemId = entry.key.jsonPrimitive.contentOrNull ?: return@EntryUpdateListener
            val isRead = entry.value.jsonPrimitive.booleanOrNull ?: return@EntryUpdateListener
            pendingReadUpdates.add(itemId to isRead)
        }
        val markedListener = EntryUpdateListener<Unit> { _, entry, _ ->
            val itemId = entry.key.jsonPrimitive.contentOrNull ?: return@EntryUpdateListener
            val isMarked = entry.value.jsonPrimitive.booleanOrNull ?: return@EntryUpdateListener
            pendingStarredUpdates.add(itemId to isMarked)
        }

        instance.addListenerWithResult(listOf("articles", "read"), readListener)
        instance.addListenerWithResult(listOf("articles", "marked"), markedListener)

        runCatching {
            instance.executeStoredEntriesForPathPrefix(listOf("articles", "read"), Unit)
            instance.executeStoredEntriesForPathPrefix(listOf("articles", "marked"), Unit)
        }.onFailure {
            logger.e("DecSync article states sync failed", it)
            return@withContext SyncResult.General(DecSyncError.SyncFeedItemsFailed)
        }

        runCatching {
            for ((itemId, isRead) in pendingReadUpdates) {
                databaseHelper.updateReadStatus(FeedItemId(itemId), isRead)
            }
            for ((itemId, isStarred) in pendingStarredUpdates) {
                databaseHelper.updateBookmarkStatus(FeedItemId(itemId), isStarred)
            }
        }.onFailure {
            logger.e("DecSync: applying article state changes to DB failed", it)
            return@withContext SyncResult.General(DecSyncError.SyncFeedItemsFailed)
        }

        SyncResult.Success
    }

    suspend fun addSubscription(url: String, title: String, categoryName: String?) {
        withContext(dispatcherProvider.io) {
            val instance = decsync ?: return@withContext
            runCatching {
                instance.setEntry(FEEDS_SUBSCRIPTIONS_PATH, JsonPrimitive(url), JsonPrimitive(true))
                instance.setEntry(FEEDS_NAMES_PATH, JsonPrimitive(url), JsonPrimitive(title))
                if (categoryName != null) {
                    instance.setEntry(
                        FEEDS_CATEGORIES_PATH,
                        JsonPrimitive(url),
                        JsonPrimitive(categoryName),
                    )
                } else {
                    instance.setEntry(FEEDS_CATEGORIES_PATH, JsonPrimitive(url), JsonNull)
                }
            }.onFailure { logger.e("DecSync: failed to add subscription $url", it) }
        }
    }

    suspend fun removeSubscription(url: String) {
        withContext(dispatcherProvider.io) {
            val instance = decsync ?: return@withContext
            runCatching {
                instance.setEntry(FEEDS_SUBSCRIPTIONS_PATH, JsonPrimitive(url), JsonPrimitive(false))
            }.onFailure { logger.e("DecSync: failed to remove subscription $url", it) }
        }
    }

    suspend fun markItemRead(itemId: String, isRead: Boolean) {
        withContext(dispatcherProvider.io) {
            val instance = decsync ?: return@withContext
            runCatching {
                instance.setEntry(articlesReadPath(), JsonPrimitive(itemId), JsonPrimitive(isRead))
            }.onFailure { logger.e("DecSync: failed to mark item $itemId read=$isRead", it) }
        }
    }

    suspend fun markItemStarred(itemId: String, isStarred: Boolean) {
        withContext(dispatcherProvider.io) {
            val instance = decsync ?: return@withContext
            runCatching {
                instance.setEntry(articlesMarkedPath(), JsonPrimitive(itemId), JsonPrimitive(isStarred))
            }.onFailure { logger.e("DecSync: failed to mark item $itemId starred=$isStarred", it) }
        }
    }

    fun disconnect() {
        decsync = null
        decSyncSettings.clearAll()
        logger.d { "DecSync disconnected" }
    }

    private suspend fun pushLocalFeedsToDecSync(instance: Decsync<Unit>) {
        val localFeeds = runCatching { databaseHelper.getFeedSources() }.getOrElse { return }
        for (feed in localFeeds) {
            runCatching {
                instance.setEntry(FEEDS_SUBSCRIPTIONS_PATH, JsonPrimitive(feed.url), JsonPrimitive(true))
                instance.setEntry(FEEDS_NAMES_PATH, JsonPrimitive(feed.url), JsonPrimitive(feed.title))
                val categoryName = feed.category?.title
                if (categoryName != null) {
                    instance.setEntry(FEEDS_CATEGORIES_PATH, JsonPrimitive(feed.url), JsonPrimitive(categoryName))
                }
            }
        }
    }

    private fun Decsync<Unit>.addListeners() {
        addListener(listOf("feeds", "subscriptions")) { _, _, _ -> }
        addListener(listOf("feeds", "names")) { _, _, _ -> }
        addListener(listOf("feeds", "categories")) { _, _, _ -> }
        addListener(listOf("articles", "read")) { _, _, _ -> }
        addListener(listOf("articles", "marked")) { _, _, _ -> }
    }

    private fun Decsync<Unit>.addListenerWithResult(
        path: List<String>,
        listener: EntryUpdateListener<Unit>,
    ) {
        addListenerWithSuccess(path) { p, entry, extra ->
            listener.onUpdate(p, entry, extra)
            true
        }
    }

    private fun articlesReadPath(): List<String> {
        val cal = Calendar.getInstance()
        return listOf(
            "articles",
            "read",
            cal.get(Calendar.YEAR).toString(),
            (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0'),
            cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'),
        )
    }

    private fun articlesMarkedPath(): List<String> {
        val cal = Calendar.getInstance()
        return listOf(
            "articles",
            "marked",
            cal.get(Calendar.YEAR).toString(),
            (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0'),
            cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'),
        )
    }

    private data class PendingFeedInsert(val url: String)

    private fun interface EntryUpdateListener<T> {
        fun onUpdate(path: List<String>, entry: Decsync.Entry, extra: T)
    }

    companion object {
        private const val SYNC_TYPE = "rss"
        private const val OWN_APP_ID = "feedflow"
        private val FEEDS_SUBSCRIPTIONS_PATH = listOf("feeds", "subscriptions")
        private val FEEDS_NAMES_PATH = listOf("feeds", "names")
        private val FEEDS_CATEGORIES_PATH = listOf("feeds", "categories")
    }
}
