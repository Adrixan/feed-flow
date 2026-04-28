package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.feedsync.decsync.DecSyncItemsSyncActions
import com.prof18.feedflow.shared.test.FakeDecSyncItemsSyncActions
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class DecSyncViewModelTest : KoinTestBase() {

    private val uiTimeout = 10.seconds
    private val fakeDecSyncActions = FakeDecSyncItemsSyncActions()
    private val viewModel: DecSyncViewModel by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + module {
            single<DecSyncItemsSyncActions> { fakeDecSyncActions }
        }

    @Test
    fun `initial state is Unlinked when not setup`() = runTest(testDispatcher) {
        fakeDecSyncActions.setupState = false

        viewModel.uiState.test(timeout = uiTimeout) {
            assertIs<AccountConnectionUiState.Unlinked>(awaitItem())
        }
    }

    @Test
    fun `initial state is Linked when already setup`() = runTest(testDispatcher) {
        fakeDecSyncActions.setupState = true

        viewModel.uiState.test(timeout = uiTimeout) {
            val state = awaitItem()
            assertIs<AccountConnectionUiState.Linked>(state)
            assertEquals(AccountSyncUIState.None, state.syncState)
        }
    }

    @Test
    fun `connect transitions through Loading then Linked on success`() = runTest(testDispatcher) {
        fakeDecSyncActions.setupResult = Result.success(Unit)

        viewModel.uiState.test(timeout = uiTimeout) {
            assertIs<AccountConnectionUiState.Unlinked>(awaitItem())

            viewModel.connect("/some/path")
            runCurrent()

            assertIs<AccountConnectionUiState.Loading>(awaitItem())

            advanceUntilIdle()

            assertIs<AccountConnectionUiState.Linked>(awaitItem())
        }
    }

    @Test
    fun `connect transitions to Unlinked on failure and emits error`() = runTest(testDispatcher) {
        val errorMessage = "Directory not found"
        fakeDecSyncActions.setupResult = Result.failure(Exception(errorMessage))

        viewModel.connect("/bad/path")
        advanceUntilIdle()

        assertEquals(AccountConnectionUiState.Unlinked, viewModel.uiState.value)
        assertEquals(errorMessage, viewModel.errorState.value)
    }

    @Test
    fun `disconnect transitions through Loading then Unlinked`() = runTest(testDispatcher) {
        fakeDecSyncActions.setupState = true

        viewModel.uiState.test(timeout = uiTimeout) {
            assertIs<AccountConnectionUiState.Linked>(awaitItem())

            viewModel.disconnect()
            runCurrent()

            assertIs<AccountConnectionUiState.Loading>(awaitItem())

            advanceUntilIdle()

            assertIs<AccountConnectionUiState.Unlinked>(awaitItem())
        }
    }

    @Test
    fun `connect with SAF content URI string succeeds`() = runTest(testDispatcher) {
        val safUri = "content://com.android.externalstorage.documents/tree/primary%3ADecSync"
        fakeDecSyncActions.setupResult = Result.success(Unit)

        viewModel.uiState.test(timeout = uiTimeout) {
            assertIs<AccountConnectionUiState.Unlinked>(awaitItem())

            viewModel.connect(safUri)
            runCurrent()

            assertIs<AccountConnectionUiState.Loading>(awaitItem())

            advanceUntilIdle()

            assertIs<AccountConnectionUiState.Linked>(awaitItem())
        }
        assertEquals(safUri, fakeDecSyncActions.lastSetupUri)
    }

    @Test
    fun `connect with SAF URI passes full URI to setup`() = runTest(testDispatcher) {
        val safUri = "content://com.android.externalstorage.documents/tree/primary%3ASync%2FDecSync"
        fakeDecSyncActions.setupResult = Result.success(Unit)

        viewModel.connect(safUri)
        advanceUntilIdle()

        assertEquals(safUri, fakeDecSyncActions.lastSetupUri)
        assertIs<AccountConnectionUiState.Linked>(viewModel.uiState.value)
    }

    @Test
    fun `connect emits connectionSuccess event on success`() = runTest(testDispatcher) {
        fakeDecSyncActions.setupResult = Result.success(Unit)

        viewModel.connectionSuccess.test(timeout = uiTimeout) {
            viewModel.connect("/some/path")
            advanceUntilIdle()
            awaitItem()
        }
    }

    @Test
    fun `connect does not emit connectionSuccess on failure`() = runTest(testDispatcher) {
        fakeDecSyncActions.setupResult = Result.failure(Exception("error"))

        viewModel.connectionSuccess.test(timeout = uiTimeout) {
            viewModel.connect("/bad/path")
            advanceUntilIdle()
            expectNoEvents()
        }
    }
}
