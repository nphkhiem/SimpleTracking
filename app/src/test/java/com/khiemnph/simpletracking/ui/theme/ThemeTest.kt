package com.khiemnph.simpletracking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.test.junit4.createComposeRule
import com.khiemnph.simpletracking.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The palette is the app's identity, so the rule protecting it is worth a test: dynamic colour is
 * opt-in, and with it off the clay red survives whatever the device wallpaper happens to be.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `dynamic colour off keeps the app's own primary, and on actually changes it`() {
        var declared = Color.Unspecified
        var ownPalette = Color.Unspecified
        var dynamic = Color.Unspecified

        // One composition: Compose allows setContent only once per test.
        composeRule.setContent {
            declared = colorResource(R.color.md_theme_primary)
            ChayNgayDiTheme(darkTheme = false, dynamicColour = false) {
                ownPalette = MaterialTheme.colorScheme.primary
            }
            ChayNgayDiTheme(darkTheme = false, dynamicColour = true) {
                dynamic = MaterialTheme.colorScheme.primary
            }
        }

        assertEquals("with the toggle off the clay palette must survive", declared, ownPalette)
        assertNotEquals("with the toggle on the scheme must actually change", ownPalette, dynamic)
    }

    /**
     * The scheme is built from `colorResource`, which resolves against the device configuration
     * rather than the `darkTheme` parameter. That is why the theme override in Settings works
     * through `AppCompatDelegate`, which changes the configuration, instead of passing a flag down.
     * Asserting on luminance rather than a hex keeps this independent of the palette's exact values.
     */
    @Test
    @Config(sdk = [33], qualifiers = "night")
    fun `a night configuration produces a dark surface`() {
        var surface = Color.Unspecified

        composeRule.setContent {
            ChayNgayDiTheme(dynamicColour = false) { surface = MaterialTheme.colorScheme.surface }
        }

        assertTrue("expected a dark surface, got $surface", surface.luminance() < 0.1f)
    }

    @Test
    fun `a day configuration produces a light surface`() {
        var surface = Color.Unspecified

        composeRule.setContent {
            ChayNgayDiTheme(dynamicColour = false) { surface = MaterialTheme.colorScheme.surface }
        }

        assertTrue("expected a light surface, got $surface", surface.luminance() > 0.8f)
    }
}
