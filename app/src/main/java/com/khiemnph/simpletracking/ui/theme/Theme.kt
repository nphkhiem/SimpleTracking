package com.khiemnph.simpletracking.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
        outline = colorResource(R.color.md_theme_outline),
        outlineVariant = colorResource(R.color.md_theme_outlineVariant),
        surfaceContainerLowest = colorResource(R.color.md_theme_surfaceContainerLowest),
        surfaceContainerLow = colorResource(R.color.md_theme_surfaceContainerLow),
        surfaceContainer = colorResource(R.color.md_theme_surfaceContainer),
        surfaceContainerHigh = colorResource(R.color.md_theme_surfaceContainerHigh),
        surfaceContainerHighest = colorResource(R.color.md_theme_surfaceContainerHighest),
    )
}

@Composable
fun ChayNgayDiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorSchemeFromResources(darkTheme),
        typography = chayNgayDiTypography(),
        shapes = ChayNgayDiShapes,
        content = content,
    )
}
