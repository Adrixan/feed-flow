package com.prof18.feedflow.desktop.accounts.decsync

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.shared.presentation.DecSyncViewModel
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import javax.swing.JFileChooser

@Composable
internal fun DecSyncSyncScreen(
    navigateBack: () -> Unit,
    showNavigateBack: Boolean = true,
) {
    val viewModel = koinViewModel<DecSyncViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorState) {
        val error = errorState ?: return@LaunchedEffect
        scope.launch {
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Long)
        }
    }

    val strings = LocalFeedFlowStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (showNavigateBack) {
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
                title = { Text("DecSync") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState) {
                is AccountConnectionUiState.Linked -> ConnectedContent(
                    onDisconnectClick = { viewModel.disconnect() },
                )
                AccountConnectionUiState.Loading -> Unit
                AccountConnectionUiState.Unlinked -> DisconnectedContent(
                    description = strings.decsyncDescription,
                    directoryHint = strings.decsyncDirectoryHint,
                    chooseDirLabel = strings.decsyncChooseDirectory,
                    onConnectClick = { dirPath -> viewModel.connect(dirPath) },
                )
            }
        }
    }
}

@Composable
private fun DisconnectedContent(
    description: String,
    directoryHint: String,
    chooseDirLabel: String,
    onConnectClick: (String) -> Unit,
) {
    var dirPath by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.regular),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular)
                .padding(top = Spacing.regular),
        ) {
            OutlinedTextField(
                value = dirPath,
                onValueChange = { dirPath = it },
                label = { Text(directoryHint) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )

            IconButton(
                onClick = {
                    scope.launch {
                        val selected = pickDirectory()
                        if (selected != null) {
                            dirPath = selected
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = chooseDirLabel,
                )
            }
        }

        SettingItem(
            modifier = Modifier.padding(top = Spacing.regular),
            title = LocalFeedFlowStrings.current.accountConnectButton,
            icon = Icons.Default.Link,
            onClick = { onConnectClick(dirPath) },
        )
    }
}

@Composable
private fun ConnectedContent(
    onDisconnectClick: () -> Unit,
) {
    Column {
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

private suspend fun pickDirectory(): String? = withContext(Dispatchers.IO) {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select DecSync Directory"
        isAcceptAllFileFilterUsed = false
    }
    val result = chooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}
