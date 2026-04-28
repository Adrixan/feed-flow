package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.feedsync.decsync.DecSyncItemsSyncActions
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DecSyncViewModel internal constructor(
    private val decSyncActions: DecSyncItemsSyncActions,
    private val accountsRepository: AccountsRepository,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    private val uiMutableState: MutableStateFlow<AccountConnectionUiState> = MutableStateFlow(
        AccountConnectionUiState.Unlinked,
    )
    val uiState = uiMutableState.asStateFlow()

    private val errorMutableState: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorState = errorMutableState.asStateFlow()

    private val connectionSuccessChannel = Channel<Unit>(Channel.BUFFERED)
    val connectionSuccess = connectionSuccessChannel.receiveAsFlow()

    init {
        if (decSyncActions.isSetup()) {
            uiMutableState.update { AccountConnectionUiState.Linked(syncState = AccountSyncUIState.None) }
        } else {
            uiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }

    fun connect(dirPath: String) {
        viewModelScope.launch {
            uiMutableState.update { AccountConnectionUiState.Loading }
            decSyncActions.setup(dirPath).fold(
                onSuccess = {
                    accountsRepository.setDecSyncAccount()
                    decSyncActions.syncFeedSources()
                    feedStateRepository.getFeeds()
                    connectionSuccessChannel.trySend(Unit)
                    uiMutableState.update { AccountConnectionUiState.Linked(syncState = AccountSyncUIState.None) }
                },
                onFailure = { error ->
                    errorMutableState.update { error.message }
                    uiMutableState.update { AccountConnectionUiState.Unlinked }
                },
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            uiMutableState.update { AccountConnectionUiState.Loading }
            decSyncActions.disconnect()
            accountsRepository.clearAccount()
            feedStateRepository.getFeeds()
            uiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }
}
