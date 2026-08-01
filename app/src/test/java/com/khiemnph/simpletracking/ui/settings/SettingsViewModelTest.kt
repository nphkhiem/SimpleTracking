package com.khiemnph.simpletracking.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.simpletracking.settings.ThemeChoice
import com.khiemnph.simpletracking.settings.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsViewModelTest {

    /**
     * One dispatcher, and therefore one scheduler, for everything this test touches: the
     * ViewModels' `viewModelScope` via `Dispatchers.Main`, the DataStore's own scope, and the test
     * body itself.
     *
     * That single ownership is the point. An earlier version put DataStore on `Dispatchers.IO`
     * while `Dispatchers.Main` was a test dispatcher, and drove the tests with `runBlocking`. A
     * test dispatcher owns a `TestCoroutineScheduler` and `runBlocking` never drains it, so a
     * continuation needing a real dispatch rather than an inline resumption could simply never
     * run. That is a lost wakeup, not slowness, which is why raising the budget from five seconds
     * to thirty did not fix it: `a second view model sees what the first one chose` still failed
     * on CI three times, once on the P8 branch and twice on main after a merge. With one scheduler
     * that `runTest` drains there is no cross-thread handoff left to lose, and no real time to
     * wait for.
     */
    private val testDispatcher = UnconfinedTestDispatcher()
    private val storeScope = CoroutineScope(SupervisorJob() + testDispatcher)
    private val viewModelStore = ViewModelStore()
    private var createdViewModels = 0
    private lateinit var repository: UserPreferencesRepository

    /**
     * Built through a [ViewModelStore] rather than by calling the constructor, so [tearDown] can
     * clear them.
     *
     * A [SettingsViewModel] collects preferences on its `viewModelScope`. Constructed directly it
     * is never cleared, so that collection outlives the test and races `Dispatchers.resetMain()`,
     * failing with "Dispatchers.Main is used concurrently with setting it".
     *
     * Each call gets its own key, because one test deliberately needs two independent instances.
     */
    private fun settingsViewModel(): SettingsViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repository) as T
        }
        return ViewModelProvider(viewModelStore, factory)["vm${createdViewModels++}", SettingsViewModel::class.java]
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = context.preferencesDataStoreFile("vm_prefs_${System.nanoTime()}")
        repository = UserPreferencesRepository(PreferenceDataStoreFactory.create(scope = storeScope) { file })
    }

    @After
    fun tearDown() {
        // Order matters: cancel the collectors, then the source they read, and only then hand
        // Dispatchers.Main back.
        viewModelStore.clear()
        storeScope.cancel()
        Dispatchers.resetMain()
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .filesDir.resolve("datastore").deleteRecursively()
    }

    @Test
    fun `starts from the stored defaults`() = runTest(testDispatcher) {
        val viewModel = settingsViewModel()

        val state = viewModel.uiState.value
        assertEquals(ThemeChoice.System, state.theme)
        assertEquals(false, state.dynamicColour)
    }

    @Test
    fun `choosing a theme is reflected back`() = runTest(testDispatcher) {
        val viewModel = settingsViewModel()

        viewModel.onThemeChosen(ThemeChoice.Dark)

        assertEquals(ThemeChoice.Dark, viewModel.uiState.first { it.theme == ThemeChoice.Dark }.theme)
    }

    @Test
    fun `turning dynamic colour on is reflected back`() = runTest(testDispatcher) {
        val viewModel = settingsViewModel()

        viewModel.onDynamicColourChanged(true)

        assertEquals(true, viewModel.uiState.first { it.dynamicColour }.dynamicColour)
    }

    @Test
    fun `a second view model sees what the first one chose`() = runTest(testDispatcher) {
        settingsViewModel().onThemeChosen(ThemeChoice.Light)
        repository.preferences.first { it.theme == ThemeChoice.Light }

        val fresh = settingsViewModel()

        assertEquals(ThemeChoice.Light, fresh.uiState.first { it.theme == ThemeChoice.Light }.theme)
    }

    @Test
    fun `reports a version to show in about`() = runTest(testDispatcher) {
        val viewModel = settingsViewModel()

        assertEquals(true, viewModel.uiState.value.versionName.isNotBlank())
    }
}
