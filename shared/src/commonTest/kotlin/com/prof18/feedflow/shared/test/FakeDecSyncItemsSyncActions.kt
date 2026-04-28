package com.prof18.feedflow.shared.test

import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.feedsync.decsync.DecSyncItemsSyncActions

class FakeDecSyncItemsSyncActions : DecSyncItemsSyncActions {
    var setupState: Boolean = false
    var setupResult: Result<Unit> = Result.success(Unit)
    var lastSetupUri: String? = null

    val markedReadItems = mutableListOf<Pair<String, Boolean>>()
    val markedStarredItems = mutableListOf<Pair<String, Boolean>>()
    val addedSubscriptions = mutableListOf<Triple<String, String, String?>>()
    val removedSubscriptions = mutableListOf<String>()

    override fun setup(dirPath: String): Result<Unit> {
        lastSetupUri = dirPath
        if (setupResult.isSuccess) setupState = true
        return setupResult
    }

    override fun isSetup(): Boolean = setupState

    override fun disconnect() {
        setupState = false
    }

    override suspend fun syncFeedSources(): SyncResult = SyncResult.Success

    override suspend fun markItemRead(itemId: String, isRead: Boolean) {
        markedReadItems.add(itemId to isRead)
    }

    override suspend fun markItemStarred(itemId: String, isStarred: Boolean) {
        markedStarredItems.add(itemId to isStarred)
    }

    override suspend fun addSubscription(url: String, title: String, categoryName: String?) {
        addedSubscriptions.add(Triple(url, title, categoryName))
    }

    override suspend fun removeSubscription(url: String) {
        removedSubscriptions.add(url)
    }
}
