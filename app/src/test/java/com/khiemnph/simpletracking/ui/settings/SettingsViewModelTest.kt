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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val viewModelStore = ViewModelStore()
    private var createdViewModels = 0
    private lateinit var repository: UserPreferencesRepository

    /**
     * Built through a [ViewModelStore] rather than by calling the constructor, so [tearDown] can
     * clear them.
     *
     * A [SettingsViewModel] collects preferences on its `viewModelScope`, which runs on
     * `Dispatchers.Main`. Constructed directly it is never cleared, so that collection outlives the
     * test and races `Dispatchers.resetMain()`, failing with "Dispatchers.Main is used concurrently
     * with setting it". Whether it lost that race depended on the order JUnit happened to run the
     * suite in, so the tests passed until an unrelated file moved between packages.
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
        Dispatchers.setMain(UnconfinedTestDispatcher())
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

    /**
     * Real time, not runTest's virtual clock. These exercise a real DataStore writing on
     * Dispatchers.IO, so a scheduler that fast-forwards would assert before the write lands.
     *
     * The budget is deliberately generous. This is real file IO on whatever hardware the tests
     * happen to run on, and a shared CI runner is far slower than a developer machine: five
     * seconds passed locally every time and still timed out on CI. The number is a guard against
     * hanging forever, not an assertion about how fast a write should be.
     */
    private fun <T> awaiting(block: suspend () -> T): T = runBlocking { withTimeout(30_000) { block() } }

    @Test
    fun `starts from the stored defaults`() = runBlocking {
        val viewModel = settingsViewModel()

        val state = viewModel.uiState.value
        assertEquals(ThemeChoice.System, state.theme)
        assertEquals(false, state.dynamicColour)
    }

    @Test
    fun `choosing a theme is reflected back`() {
        val viewModel = settingsViewModel()

        viewModel.onThemeChosen(ThemeChoice.Dark)

        val state = awaiting { viewModel.uiState.first { it.theme == ThemeChoice.Dark } }
        assertEquals(ThemeChoice.Dark, state.theme)
    }

    @Test
    fun `turning dynamic colour on is reflected back`() {
        val viewModel = settingsViewModel()

        viewModel.onDynamicColourChanged(true)

        val state = awaiting { viewModel.uiState.first { it.dynamicColour } }
        assertEquals(true, state.dynamicColour)
    }

    @Test
    fun `a second view model sees what the first one chose`() {
        settingsViewModel().onThemeChosen(ThemeChoice.Light)
        awaiting { repository.preferences.first { it.theme == ThemeChoice.Light } }

        val fresh = settingsViewModel()

        val state = awaiting { fresh.uiState.first { it.theme == ThemeChoice.Light } }
        assertEquals(ThemeChoice.Light, state.theme)
    }

    @Test
    fun `reports a version to show in about`() = runBlocking {
        val viewModel = settingsViewModel()

        assertEquals(true, viewModel.uiState.value.versionName.isNotBlank())
    }
}
