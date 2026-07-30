package com.khiemnph.simpletracking.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.khiemnph.simpletracking.settings.ThemeChoice
import com.khiemnph.simpletracking.testing.DefaultLocaleRule
import com.khiemnph.simpletracking.ui.theme.ChayNgayDiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsScreenTest {

    @get:Rule
    val localeRule = DefaultLocaleRule()

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        state: SettingsUiState = SettingsUiState(versionName = "1.0"),
        onThemeChosen: (ThemeChoice) -> Unit = {},
        onDynamicColourChanged: (Boolean) -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChayNgayDiTheme {
                SettingsScreen(
                    state = state,
                    onThemeChosen = onThemeChosen,
                    onDynamicColourChanged = onDynamicColourChanged,
                    onBack = onBack,
                )
            }
        }
    }

    @Test
    fun `offers all three theme choices`() {
        render()

        ThemeChoice.entries.forEach {
            composeRule.onNodeWithTag(SettingsTestTags.themeOption(it)).assertIsDisplayed()
        }
    }

    @Test
    fun `shows which theme is currently chosen`() {
        render(SettingsUiState(theme = ThemeChoice.Dark, versionName = "1.0"))

        composeRule.onNodeWithTag(SettingsTestTags.themeOption(ThemeChoice.Dark)).assertIsSelected()
    }

    @Test
    fun `choosing a theme reports it`() {
        var chosen: ThemeChoice? = null
        render(onThemeChosen = { chosen = it })

        composeRule.onNodeWithTag(SettingsTestTags.themeOption(ThemeChoice.Light)).performClick()

        assertEquals(ThemeChoice.Light, chosen)
    }

    @Test
    fun `dynamic colour reads as off by default, which is the brief's whole point`() {
        render(SettingsUiState(dynamicColour = false, versionName = "1.0"))

        composeRule.onNodeWithTag(SettingsTestTags.DYNAMIC_COLOUR).assertIsOff()
    }

    @Test
    fun `dynamic colour reads as on when enabled`() {
        render(SettingsUiState(dynamicColour = true, versionName = "1.0"))

        composeRule.onNodeWithTag(SettingsTestTags.DYNAMIC_COLOUR).assertIsOn()
    }

    @Test
    fun `toggling dynamic colour reports the new value`() {
        var enabled: Boolean? = null
        render(SettingsUiState(dynamicColour = false, versionName = "1.0"), onDynamicColourChanged = { enabled = it })

        composeRule.onNodeWithTag(SettingsTestTags.DYNAMIC_COLOUR).performClick()

        assertEquals(true, enabled)
    }

    @Test
    fun `shows the app version, scrolling to it if the screen is short`() {
        render(SettingsUiState(versionName = "1.0"))

        composeRule.onNodeWithTag(SettingsTestTags.VERSION).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `going back reports it`() {
        var back = 0
        render(onBack = { back++ })

        composeRule.onNodeWithTag(SettingsTestTags.BACK).performClick()

        assertEquals(1, back)
    }
}
