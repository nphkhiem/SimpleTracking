package com.khiemnph.simpletracking.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.khiemnph.simpletracking.R

/**
 * The Compose half of `Theme.ChayNgayDi`.
 *
 * Every colour is read from the same `values/themes.xml` and `values-night/themes.xml` roles the
 * View layer uses, via [colorResource], rather than being restated as Kotlin hex literals. While
 * both layers exist side by side that is the only way they cannot drift: there is one definition of
 * "primary in dark mode", not two that have to be kept in step by hand. It costs a resource lookup
 * per role at theme construction, which happens once per composition of [ChayNgayDiTheme].
 *
 * Dynamic colour is deliberately absent. It is a Settings opt-in, not a default, because pulling
 * the palette from the user's wallpaper would discard the warm clay identity the app is built on.
 */
@Composable
private fun colorSchemeFromResources(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colorResource(R.color.md_theme_primary),
        onPrimary = colorResource(R.color.md_theme_onPrimary),
        primaryContainer = colorResource(R.color.md_theme_primaryContainer),
        onPrimaryContainer = colorResource(R.color.md_theme_onPrimaryContainer),
        secondary = colorResource(R.color.md_theme_secondary),
        onSecondary = colorResource(R.color.md_theme_onSecondary),
        secondaryContainer = colorResource(R.color.md_theme_secondaryContainer),
        onSecondaryContainer = colorResource(R.color.md_theme_onSecondaryContainer),
        tertiary = colorResource(R.color.md_theme_tertiary),
        onTertiary = colorResource(R.color.md_theme_onTertiary),
        tertiaryContainer = colorResource(R.color.md_theme_tertiaryContainer),
        onTertiaryContainer = colorResource(R.color.md_theme_onTertiaryContainer),
        error = colorResource(R.color.md_theme_error),
        onError = colorResource(R.color.md_theme_onError),
        errorContainer = colorResource(R.color.md_theme_errorContainer),
        onErrorContainer = colorResource(R.color.md_theme_onErrorContainer),
        background = colorResource(R.color.md_theme_background),
        onBackground = colorResource(R.color.md_theme_onBackground),
        surface = colorResource(R.color.md_theme_surface),
        onSurface = colorResource(R.color.md_theme_onSurface),
        surfaceVariant = colorResource(R.color.md_theme_surfaceVariant),
        onSurfaceVariant = colorResource(R.color.md_theme_onSurfaceVariant),
        // The inverse roles are what a snackbar is built from. Leaving them unmapped is not
        // neutral: Compose falls back to Material's defaults, and the Undo action renders in
        // baseline purple against this palette.
        inversePrimary = colorResource(R.color.md_theme_primaryInverse),
        inverseSurface = colorResource(R.color.md_theme_surfaceInverse),
        inverseOnSurface = colorResource(R.color.md_theme_onSurfaceInverse),
        outline = colorResource(R.color.md_theme_outline),
        outlineVariant = colorResource(R.color.md_theme_outlineVariant),
        surfaceContainerLowest = colorResource(R.color.md_theme_surfaceContainerLowest),
        surfaceContainerLow = colorResource(R.color.md_theme_surfaceContainerLow),
        surfaceContainer = colorResource(R.color.md_theme_surfaceContainer),
        surfaceContainerHigh = colorResource(R.color.md_theme_surfaceContainerHigh),
        surfaceContainerHighest = colorResource(R.color.md_theme_surfaceContainerHighest),
    )
}

/**
 * [dynamicColour] is off by default and stays that way unless the user asks for it in Settings.
 *
 * The brief's reasoning: this app's visual identity *is* its palette, and handing that to whatever
 * the wallpaper happens to be on first launch would delete the one part of the design that was
 * already good. Offering it is a courtesy to people who want their phone to match; assuming it is
 * not.
 *
 * Below Android 12 the platform cannot extract a scheme at all, so the request is simply ignored
 * rather than failing.
 */
@Composable
fun ChayNgayDiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColour: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColour && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> colorSchemeFromResources(darkTheme)
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = chayNgayDiTypography(),
        shapes = ChayNgayDiShapes,
        content = content,
    )
}
