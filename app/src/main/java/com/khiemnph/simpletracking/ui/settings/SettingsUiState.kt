package com.khiemnph.simpletracking.ui.settings

import com.khiemnph.simpletracking.settings.ThemeChoice

data class SettingsUiState(
    val theme: ThemeChoice = ThemeChoice.System,
    val dynamicColour: Boolean = false,
    val versionName: String = "",
)
