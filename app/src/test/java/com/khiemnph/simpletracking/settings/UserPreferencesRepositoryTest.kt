package com.khiemnph.simpletracking.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The preference store's own behaviour, tested through the repository rather than by reading
 * DataStore keys back directly. What matters is that a setting survives a write and that a fresh
 * install starts with the defaults the brief asks for.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UserPreferencesRepositoryTest {

    private lateinit var repository: UserPreferencesRepository
    /**
     * A real scope, not a TestScope. DataStore runs an internal actor on whatever scope it is
     * given, and a TestScope that nothing advances leaves that actor unstarted, so the first read
     * suspends forever rather than failing.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = context.preferencesDataStoreFile("test_prefs_${System.nanoTime()}")
        repository = UserPreferencesRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @After
    fun tearDown() {
        scope.cancel()
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .filesDir.resolve("datastore").deleteRecursively()
    }

    @Test
    fun `a fresh install follows the system theme`() = runTest {
        assertEquals(ThemeChoice.System, repository.preferences.first().theme)
    }

    @Test
    fun `a fresh install has dynamic colour off, so the app keeps its own palette`() = runTest {
        // The brief is explicit: an app whose identity is its palette must not surrender it to a
        // wallpaper on first launch. Dynamic colour is offered, not assumed.
        assertEquals(false, repository.preferences.first().dynamicColour)
    }

    @Test
    fun `a chosen theme is remembered`() = runTest {
        repository.setTheme(ThemeChoice.Dark)

        assertEquals(ThemeChoice.Dark, repository.preferences.first().theme)
    }

    @Test
    fun `dynamic colour can be turned on and back off`() = runTest {
        repository.setDynamicColour(true)
        assertEquals(true, repository.preferences.first().dynamicColour)

        repository.setDynamicColour(false)
        assertEquals(false, repository.preferences.first().dynamicColour)
    }

    @Test
    fun `each setting is independent of the other`() = runTest {
        repository.setTheme(ThemeChoice.Light)
        repository.setDynamicColour(true)

        val preferences = repository.preferences.first()
        assertEquals(ThemeChoice.Light, preferences.theme)
        assertEquals(true, preferences.dynamicColour)
    }
}
