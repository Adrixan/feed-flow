package com.prof18.feedflow.feedsync.decsync.di

import com.prof18.feedflow.feedsync.decsync.DecSyncItemsSyncActions
import com.prof18.feedflow.feedsync.decsync.DecSyncItemsSyncActionsImpl
import com.prof18.feedflow.feedsync.decsync.DecSyncRepository
import com.prof18.feedflow.feedsync.decsync.DecSyncSettings
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

actual val decSyncModule = module {
    single {
        DecSyncSettings(settings = get())
    }

    single {
        DecSyncRepository(
            decSyncSettings = get(),
            databaseHelper = get(),
            dispatcherProvider = get(),
            logger = get(parameters = { parametersOf("DecSyncRepository") }),
        )
    }

    single<DecSyncItemsSyncActions> {
        DecSyncItemsSyncActionsImpl(
            decSyncRepository = get(),
        )
    }
}
