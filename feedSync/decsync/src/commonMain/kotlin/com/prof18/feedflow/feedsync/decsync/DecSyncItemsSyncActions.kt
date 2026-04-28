package com.prof18.feedflow.feedsync.decsync

interface DecSyncItemsSyncActions {
    fun setup(dirPath: String): Result<Unit>
    fun isSetup(): Boolean
    fun disconnect()
    suspend fun markItemRead(itemId: String, isRead: Boolean)
    suspend fun markItemStarred(itemId: String, isStarred: Boolean)
    suspend fun addSubscription(url: String, title: String, categoryName: String?)
    suspend fun removeSubscription(url: String)
}
