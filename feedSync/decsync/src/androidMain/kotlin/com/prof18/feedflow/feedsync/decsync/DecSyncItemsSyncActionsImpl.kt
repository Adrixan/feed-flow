package com.prof18.feedflow.feedsync.decsync

import android.content.Context
import com.prof18.feedflow.core.model.SyncResult

internal class DecSyncItemsSyncActionsImpl(
    private val context: Context,
    private val decSyncRepository: DecSyncRepository,
) : DecSyncItemsSyncActions {
    init {
        decSyncRepository.restoreIfConfigured(context)
    }

    override fun setup(dirPath: String): Result<Unit> = decSyncRepository.setup(context, dirPath)
    override fun isSetup(): Boolean = decSyncRepository.isDecSyncSetup()
    override fun disconnect() = decSyncRepository.disconnect()
    override suspend fun syncFeedSources(): SyncResult = decSyncRepository.syncFeedSources()
    override suspend fun markItemRead(itemId: String, isRead: Boolean) {
        decSyncRepository.markItemRead(itemId, isRead)
    }

    override suspend fun markItemStarred(itemId: String, isStarred: Boolean) {
        decSyncRepository.markItemStarred(itemId, isStarred)
    }

    override suspend fun addSubscription(url: String, title: String, categoryName: String?) {
        decSyncRepository.addSubscription(url, title, categoryName)
    }

    override suspend fun removeSubscription(url: String) {
        decSyncRepository.removeSubscription(url)
    }
}
