package com.khiemnph.simpletracking.settings

import androidx.appcompat.app.AppCompatDelegate
import com.khiemnph.simpletracking.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Applies the stored theme choice to the whole app.
 *
 * Through `AppCompatDelegate` rather than by passing a flag into the theme, because the colour
 * scheme is built from `colorResource`, which resolves `values-night` against the device
 * configuration. Changing the configuration is therefore the only thing that actually switches the
 * palette, and it also gets the View-based Record screen for free.
 */
@Singleton
class ThemeApplier @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    fun start() {
        applicationScope.launch {
            userPreferencesRepository.preferences
                .map { it.theme }
                .distinctUntilChanged()
                .collect { theme ->
                    // On the main thread, deliberately. setDefaultNightMode recreates the live
                    // activities, and from a background thread it stores the value without
                    // applying it: the preference sticks but the app never changes appearance.
                    withContext(Dispatchers.Main) {
                        AppCompatDelegate.setDefaultNightMode(theme.toNightMode())
                    }
                }
        }
    }

    private fun ThemeChoice.toNightMode(): Int = when (this) {
        ThemeChoice.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeChoice.Light -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeChoice.Dark -> AppCompatDelegate.MODE_NIGHT_YES
    }
}
