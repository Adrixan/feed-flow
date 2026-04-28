package com.prof18.feedflow.feedsync.decsync.di

import com.prof18.feedflow.feedsync.decsync.DecSyncItemsSyncActions
import com.prof18.feedflow.feedsync.decsync.DecSyncSettings
import com.prof18.feedflow.feedsync.decsync.NoOpDecSyncItemsSyncActions
import org.koin.dsl.module

actual val decSyncModule = module {
    single { DecSyncSettings(settings = get()) }
    single<DecSyncItemsSyncActions> { NoOpDecSyncItemsSyncActions }
}
