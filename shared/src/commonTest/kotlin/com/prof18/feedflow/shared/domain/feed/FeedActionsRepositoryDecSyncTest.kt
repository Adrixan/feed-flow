package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.decsync.DecSyncItemsSyncActions
import com.prof18.feedflow.feedsync.decsync.DecSyncSettings
import com.prof18.feedflow.shared.test.FakeDecSyncItemsSyncActions
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedActionsRepositoryDecSyncTest : KoinTestBase() {

    private val feedActionsRepository: FeedActionsRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val fakeDecSyncActions = FakeDecSyncItemsSyncActions()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + module {
            single<DecSyncItemsSyncActions> { fakeDecSyncActions }
        }

    private fun setupDecSyncAccount() {
        val settings: DecSyncSettings = getKoin().get()
        settings.setDirPath("/some/decsync/path")
    }

    @Test
    fun `markAsRead should call decSync markItemRead and update database`() = runTest(testDispatcher) {
        setupDecSyncAccount()
        val feedSource = createFeedSource("source-1", "Test Feed")
        databaseHelper.insertFeedSourceWithCategory(feedSource)

        val feedItems = listOf(
            buildFeedItem("item-1", "Article 1", 10000L, feedSource),
            buildFeedItem("item-2", "Article 2", 9000L, feedSource),
        )
        databaseHelper.insertFeedItems(feedItems, lastSyncTimestamp = 0)
        feedStateRepository.getFeeds()
        advanceUntilIdle()

        val itemIds = feedItems.map { FeedItemId(it.id) }.toHashSet()
        feedActionsRepository.markAsRead(itemIds)
        advanceUntilIdle()

        val updatedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        itemIds.forEach { itemId ->
            val item = updatedItems.find { it.url_hash == itemId.id }
            assertNotNull(item, "Item ${itemId.id} should exist")
            assertTrue(item.is_read, "Item ${itemId.id} should be marked as read")
        }

        assertEquals(feedItems.size, fakeDecSyncActions.markedReadItems.size)
        fakeDecSyncActions.markedReadItems.forEach { (_, isRead) ->
            assertTrue(isRead, "markItemRead should be called with isRead=true")
        }
    }

    @Test
    fun `updateBookmarkStatus true should call decSync markItemStarred and update database`() =
        runTest(testDispatcher) {
            setupDecSyncAccount()
            val feedSource = createFeedSource("source-1", "Test Feed")
            databaseHelper.insertFeedSourceWithCategory(feedSource)

            val feedItem = buildFeedItem("item-1", "Article 1", 10000L, feedSource)
            databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
            feedStateRepository.getFeeds()
            advanceUntilIdle()

            feedActionsRepository.updateBookmarkStatus(FeedItemId(feedItem.id), isBookmarked = true)
            advanceUntilIdle()

            val updatedItems = databaseHelper.getFeedItems(
                feedFilter = FeedFilter.Timeline,
                pageSize = 10,
                offset = 0,
                showReadItems = true,
                sortOrder = FeedOrder.NEWEST_FIRST,
            )
            val updatedItem = updatedItems.find { it.url_hash == feedItem.id }
            assertNotNull(updatedItem)
            assertTrue(updatedItem.is_bookmarked, "Item should be bookmarked in database")

            assertEquals(1, fakeDecSyncActions.markedStarredItems.size)
            val (id, isStarred) = fakeDecSyncActions.markedStarredItems.first()
            assertEquals(feedItem.id, id)
            assertTrue(isStarred, "markItemStarred should be called with isStarred=true")
        }

    @Test
    fun `updateBookmarkStatus false should call decSync markItemStarred with false and update database`() =
        runTest(testDispatcher) {
            setupDecSyncAccount()
            val feedSource = createFeedSource("source-1", "Test Feed")
            databaseHelper.insertFeedSourceWithCategory(feedSource)

            val feedItem = buildFeedItem("item-1", "Article 1", 10000L, feedSource)
            databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
            databaseHelper.updateBookmarkStatus(feedItemId = FeedItemId(feedItem.id), isBookmarked = true)
            feedStateRepository.getFeeds()
            advanceUntilIdle()

            feedActionsRepository.updateBookmarkStatus(FeedItemId(feedItem.id), isBookmarked = false)
            advanceUntilIdle()

            val updatedItems = databaseHelper.getFeedItems(
                feedFilter = FeedFilter.Timeline,
                pageSize = 10,
                offset = 0,
                showReadItems = true,
                sortOrder = FeedOrder.NEWEST_FIRST,
            )
            val updatedItem = updatedItems.find { it.url_hash == feedItem.id }
            assertNotNull(updatedItem)
            assertFalse(updatedItem.is_bookmarked, "Item should not be bookmarked in database")

            assertEquals(1, fakeDecSyncActions.markedStarredItems.size)
            val (_, isStarred) = fakeDecSyncActions.markedStarredItems.first()
            assertFalse(isStarred, "markItemStarred should be called with isStarred=false")
        }

    @Test
    fun `updateReadStatus true should call decSync markItemRead and update database`() =
        runTest(testDispatcher) {
            setupDecSyncAccount()
            val feedSource = createFeedSource("source-1", "Test Feed")
            databaseHelper.insertFeedSourceWithCategory(feedSource)

            val feedItem = buildFeedItem("item-1", "Article 1", 10000L, feedSource)
            databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
            feedStateRepository.getFeeds()
            advanceUntilIdle()

            feedActionsRepository.updateReadStatus(FeedItemId(feedItem.id), isRead = true)
            advanceUntilIdle()

            val updatedItems = databaseHelper.getFeedItems(
                feedFilter = FeedFilter.Timeline,
                pageSize = 10,
                offset = 0,
                showReadItems = true,
                sortOrder = FeedOrder.NEWEST_FIRST,
            )
            val updatedItem = updatedItems.find { it.url_hash == feedItem.id }
            assertNotNull(updatedItem)
            assertTrue(updatedItem.is_read, "Item should be marked as read in database")

            assertEquals(1, fakeDecSyncActions.markedReadItems.size)
            val (id, isRead) = fakeDecSyncActions.markedReadItems.first()
            assertEquals(feedItem.id, id)
            assertTrue(isRead, "markItemRead should be called with isRead=true")
        }

    @Test
    fun `updateReadStatus false should call decSync markItemRead with false and update database`() =
        runTest(testDispatcher) {
            setupDecSyncAccount()
            val feedSource = createFeedSource("source-1", "Test Feed")
            databaseHelper.insertFeedSourceWithCategory(feedSource)

            val feedItem = buildFeedItem("item-1", "Article 1", 10000L, feedSource)
            databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)
            databaseHelper.updateReadStatus(feedItemId = FeedItemId(feedItem.id), isRead = true)
            feedStateRepository.getFeeds()
            advanceUntilIdle()

            feedActionsRepository.updateReadStatus(FeedItemId(feedItem.id), isRead = false)
            advanceUntilIdle()

            val updatedItems = databaseHelper.getFeedItems(
                feedFilter = FeedFilter.Timeline,
                pageSize = 10,
                offset = 0,
                showReadItems = true,
                sortOrder = FeedOrder.NEWEST_FIRST,
            )
            val updatedItem = updatedItems.find { it.url_hash == feedItem.id }
            assertNotNull(updatedItem)
            assertFalse(updatedItem.is_read, "Item should be unread in database")

            assertEquals(1, fakeDecSyncActions.markedReadItems.size)
            val (_, isRead) = fakeDecSyncActions.markedReadItems.first()
            assertFalse(isRead, "markItemRead should be called with isRead=false")
        }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/$id/feed.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com/$id",
        fetchFailed = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
