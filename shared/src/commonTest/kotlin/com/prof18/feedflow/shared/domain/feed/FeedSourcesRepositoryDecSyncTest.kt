package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.decsync.DecSyncItemsSyncActions
import com.prof18.feedflow.feedsync.decsync.DecSyncSettings
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.test.FakeDecSyncItemsSyncActions
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedSourcesRepositoryDecSyncTest : KoinTestBase() {

    private val feedSourcesRepository: FeedSourcesRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val fakeDecSyncActions = FakeDecSyncItemsSyncActions()
    private val fakeRssParserWrapper = FakeRssParserWrapper()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + module {
            single<DecSyncItemsSyncActions> { fakeDecSyncActions }
            single<RssParserWrapper> { fakeRssParserWrapper }
        }

    private fun setupDecSyncAccount() {
        val settings: DecSyncSettings = getKoin().get()
        settings.setDirPath("/some/decsync/path")
    }

    @Test
    fun `deleteFeed should call decSync removeSubscription and delete feed source from database`() =
        runTest(testDispatcher) {
            setupDecSyncAccount()
            val feedSource = createFeedSource("source-1", "Test Feed")
            databaseHelper.insertFeedSourceWithCategory(feedSource)
            advanceUntilIdle()

            feedSourcesRepository.deleteFeed(feedSource)
            advanceUntilIdle()

            val deletedFeedSource = databaseHelper.getFeedSource(feedSource.id)
            assertNull(deletedFeedSource, "Feed source should be deleted from database")

            assertEquals(1, fakeDecSyncActions.removedSubscriptions.size)
            assertEquals(feedSource.url, fakeDecSyncActions.removedSubscriptions.first())
        }

    @Test
    fun `updateFeedSourceName should call decSync addSubscription with new name`() =
        runTest(testDispatcher) {
            setupDecSyncAccount()
            val feedSource = createFeedSource("source-1", "Original Name")
            databaseHelper.insertFeedSourceWithCategory(feedSource)
            advanceUntilIdle()

            feedSourcesRepository.updateFeedSourceName(feedSource.id, "Updated Name")
            advanceUntilIdle()

            val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
            assertTrue(updatedFeedSource != null, "Feed source should exist")
            assertEquals("Updated Name", updatedFeedSource.title, "Feed source name should be updated in database")

            assertEquals(1, fakeDecSyncActions.addedSubscriptions.size)
            val (url, title, _) = fakeDecSyncActions.addedSubscriptions.first()
            assertEquals(feedSource.url, url)
            assertEquals("Updated Name", title)
        }

    @Test
    fun `addFeedSource should call decSync addSubscription and return FeedAdded`() =
        runTest(testDispatcher) {
            setupDecSyncAccount()
            val feedUrl = "https://example.com/feed.xml"
            fakeRssParserWrapper.feedUrl = feedUrl

            val result = feedSourcesRepository.addFeedSource(
                feedUrl = feedUrl,
                categoryName = null,
                isNotificationEnabled = false,
            )
            advanceUntilIdle()

            assertIs<FeedAddedState.FeedAdded>(result, "Should return FeedAdded on success")

            assertEquals(1, fakeDecSyncActions.addedSubscriptions.size)
            val (url, _, _) = fakeDecSyncActions.addedSubscriptions.first()
            assertEquals(feedUrl, url)
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

    private class FakeRssParserWrapper : RssParserWrapper {
        var feedUrl: String? = null

        override suspend fun getRssChannel(url: String): RssChannel = RssChannel(
            title = "Example Feed",
            link = feedUrl ?: url,
            description = null,
            image = null,
            lastBuildDate = null,
            updatePeriod = null,
            items = emptyList(),
            itunesChannelData = null,
            youtubeChannelData = null,
        )
    }
}
