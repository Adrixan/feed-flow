package com.prof18.feedflow.android.accounts.decsync

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.shared.presentation.DecSyncViewModel
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DecSyncSyncScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<DecSyncViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val strings = LocalFeedFlowStrings.current

    LaunchedEffect(errorState) {
        val error = errorState ?: return@LaunchedEffect
        scope.launch {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long,
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.connectionSuccess.collect {
            snackbarHostState.showSnackbar(
                message = strings.decsyncAccountConnected,
                duration = SnackbarDuration.Short,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text("DecSync") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (uiState) {
            is AccountConnectionUiState.Linked -> ConnectedView(
                modifier = Modifier.padding(padding),
                onDisconnectClick = { viewModel.disconnect() },
            )

            AccountConnectionUiState.Loading -> Unit

            AccountConnectionUiState.Unlinked -> DisconnectedView(
                modifier = Modifier.padding(padding),
                onConnectWithUri = { uri -> viewModel.connect(uri) },
            )
        }
    }
}

@Composable
private fun DisconnectedView(
    onConnectWithUri: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strings = LocalFeedFlowStrings.current
    var selectedUri by remember { mutableStateOf<String?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            selectedUri = uri.toString()
        }
    }

    Column(modifier = modifier) {
        Text(
            text = strings.decsyncDescription,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.regular),
        )

        Text(
            text = selectedUri ?: strings.decsyncDirectoryHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = Spacing.regular)
                .padding(top = Spacing.regular),
        )

        SettingItem(
            modifier = Modifier.padding(top = Spacing.small),
            title = strings.decsyncChooseDirectory,
            icon = Icons.Default.FolderOpen,
            onClick = { folderPickerLauncher.launch(null) },
        )

        if (selectedUri != null) {
            Button(
                onClick = { onConnectWithUri(selectedUri!!) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.regular)
                    .padding(top = Spacing.regular),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Spacing.small),
                )
                Text(text = strings.decsyncConnect)
            }
        }
    }
}

@Composable
private fun ConnectedView(
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = LocalFeedFlowStrings.current.decsyncAccountConnected,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.regular),
        )

        SettingItem(
            modifier = Modifier.padding(top = Spacing.regular),
            title = LocalFeedFlowStrings.current.accountDisconnectButton,
            icon = Icons.Default.LinkOff,
            onClick = onDisconnectClick,
        )
    }
}
