package com.prof18.feedflow.feedsync.decsync

internal object NoOpDecSyncItemsSyncActions : DecSyncItemsSyncActions {
    override fun setup(dirPath: String): Result<Unit> = Result.success(Unit)
    override fun isSetup(): Boolean = false
    override fun disconnect() = Unit
    override suspend fun markItemRead(itemId: String, isRead: Boolean) = Unit
    override suspend fun markItemStarred(itemId: String, isStarred: Boolean) = Unit
    override suspend fun addSubscription(url: String, title: String, categoryName: String?) = Unit
    override suspend fun removeSubscription(url: String) = Unit
}
