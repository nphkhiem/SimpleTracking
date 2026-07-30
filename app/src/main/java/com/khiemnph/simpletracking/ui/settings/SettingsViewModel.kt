package com.khiemnph.simpletracking.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khiemnph.simpletracking.BuildConfig
import com.khiemnph.simpletracking.settings.ThemeChoice
import com.khiemnph.simpletracking.settings.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the Settings screen.
 *
 * State comes from the stored preferences rather than from what the buttons were last set to, so
 * the screen shows what the app will actually do, including after a change made elsewhere.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(versionName = BuildConfig.VERSION_NAME))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.preferences.collect { preferences ->
                _uiState.value = _uiState.value.copy(
                    theme = preferences.theme,
                    dynamicColour = preferences.dynamicColour,
                )
            }
        }
    }

    fun onThemeChosen(theme: ThemeChoice) {
        viewModelScope.launch { userPreferencesRepository.setTheme(theme) }
    }

    fun onDynamicColourChanged(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setDynamicColour(enabled) }
    }
}
