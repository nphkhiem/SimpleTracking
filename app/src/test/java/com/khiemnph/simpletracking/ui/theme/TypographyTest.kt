package com.khiemnph.simpletracking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isUnspecified
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards a crash that reached the app once already.
 *
 * Compose cannot interpolate a letter spacing given in `em` with one given in `sp`, and Material's
 * own styles use `sp`. Any component that animates between two type styles hits that, so a single
 * `em` value anywhere in the scale is enough to crash every labelled `OutlinedTextField` in the app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TypographyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun stylesOf(typography: Typography) = listOf(
        "displayLarge" to typography.displayLarge,
        "displayMedium" to typography.displayMedium,
        "displaySmall" to typography.displaySmall,
        "headlineLarge" to typography.headlineLarge,
        "headlineMedium" to typography.headlineMedium,
        "headlineSmall" to typography.headlineSmall,
        "titleLarge" to typography.titleLarge,
        "titleMedium" to typography.titleMedium,
        "titleSmall" to typography.titleSmall,
        "bodyLarge" to typography.bodyLarge,
        "bodyMedium" to typography.bodyMedium,
        "bodySmall" to typography.bodySmall,
        "labelLarge" to typography.labelLarge,
        "labelMedium" to typography.labelMedium,
        "labelSmall" to typography.labelSmall,
    )

    @Test
    fun `no style in the scale measures letter spacing in em`() {
        var offenders: List<String> = emptyList()

        composeRule.setContent {
            ChayNgayDiTheme {
                val typography = MaterialTheme.typography
                offenders = stylesOf(typography)
                    .filter { (_, style) ->
                        !style.letterSpacing.isUnspecified && style.letterSpacing.type == TextUnitType.Em
                    }
                    .map { (name, style) -> "$name=${style.letterSpacing}" }
                Text("probe")
            }
        }

        assertTrue("mixing em and sp crashes any animated text style: $offenders", offenders.isEmpty())
    }

    @Test
    fun `a text style can be interpolated with another, which is what animating between them does`() {
        // Reproduces the actual failure path rather than only inspecting units: lerp is what
        // OutlinedTextField's floating label performs, and it threw before the scale was made
        // consistent.
        composeRule.setContent {
            ChayNgayDiTheme {
                var progress by remember { mutableStateOf(0f) }
                val from = MaterialTheme.typography.bodyLarge
                val to = MaterialTheme.typography.bodySmall
                progress = 0.5f
                Text(text = "probe", style = androidx.compose.ui.text.lerp(from, to, progress))
            }
        }

        composeRule.waitForIdle()
    }
}
