package com.khiemnph.simpletracking.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG AA: 4.5:1 for body text, 3:1 for large text and for meaningful non-text. */
private const val AA_BODY = 4.5
private const val AA_LARGE = 3.0

/**
 * Every colour role the app actually paints text with, checked in both themes.
 *
 * This is one of the brief's ten success criteria, and it is the one that had already been broken
 * once: the old History list measured 1.1:1 in dark mode, which is invisible rather than merely
 * poor. Asserting it here means a future palette change cannot quietly reintroduce that.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ContrastTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun relativeLuminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun ratio(foreground: Color, background: Color): Double {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private data class Pairing(
        val name: String,
        val foreground: Color,
        val background: Color,
        val minimum: Double,
    )

    /** Foreground and background pairs the app genuinely renders together. */
    private fun pairsOf(scheme: ColorScheme): List<Pairing> = listOf(
        Pairing("onSurface on surface", scheme.onSurface, scheme.surface, AA_BODY),
        Pairing("onSurfaceVariant on surface", scheme.onSurfaceVariant, scheme.surface, AA_BODY),
        Pairing("onSurface on surfaceContainerLow", scheme.onSurface, scheme.surfaceContainerLow, AA_BODY),
        Pairing("onSurface on surfaceContainerHigh", scheme.onSurface, scheme.surfaceContainerHigh, AA_BODY),
        Pairing("onPrimary on primary", scheme.onPrimary, scheme.primary, AA_BODY),
        Pairing("onError on error", scheme.onError, scheme.error, AA_BODY),
        Pairing("onErrorContainer on errorContainer", scheme.onErrorContainer, scheme.errorContainer, AA_BODY),
        Pairing("error on surface", scheme.error, scheme.surface, AA_BODY),
        Pairing("inverseOnSurface on inverseSurface", scheme.inverseOnSurface, scheme.inverseSurface, AA_BODY),
        // The route line and the week-strip bars carry meaning, so they need the non-text minimum.
        Pairing("primary on surface", scheme.primary, scheme.surface, AA_LARGE),
        Pairing("primary on surfaceContainerLow", scheme.primary, scheme.surfaceContainerLow, AA_LARGE),
        Pairing("outline on surface", scheme.outline, scheme.surface, AA_LARGE),
    )

    private fun failuresIn(scheme: ColorScheme): List<String> =
        pairsOf(scheme).mapNotNull { pairing ->
            val measured = ratio(pairing.foreground, pairing.background)
            if (measured < pairing.minimum) {
                "%s is %.2f:1, needs %.1f:1".format(pairing.name, measured, pairing.minimum)
            } else {
                null
            }
        }

    @Test
    fun `light mode meets AA everywhere text is drawn`() {
        var failures: List<String> = emptyList()

        composeRule.setContent {
            ChayNgayDiTheme(darkTheme = false, dynamicColour = false) {
                failures = failuresIn(MaterialTheme.colorScheme)
            }
        }

        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    @Config(sdk = [33], qualifiers = "night")
    fun `dark mode meets AA everywhere text is drawn`() {
        var failures: List<String> = emptyList()

        composeRule.setContent {
            ChayNgayDiTheme(dynamicColour = false) {
                failures = failuresIn(MaterialTheme.colorScheme)
            }
        }

        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
