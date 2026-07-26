package com.khiemnph.simpletracking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.khiemnph.simpletracking.R

/**
 * The Compose type scale, matching `values/type.xml` value for value.
 *
 * Unlike the colour roles, these cannot be read from the XML styles: there is no Compose API that
 * resolves a `TextAppearance` into a [TextStyle]. The sizes are therefore restated here, and the
 * two must be changed together until the View layer is gone. Each figure below appears in exactly
 * one place in `type.xml`, so a mismatch is a visible diff rather than a silent drift.
 *
 * Letter spacing is expressed in `em` because that is the unit `android:letterSpacing` uses; the
 * XML value carries across unchanged rather than being converted to `sp`.
 */
@Composable
fun chayNgayDiTypography(): Typography {
    val base = MaterialTheme.typography
    return base.copy(
        displayLarge = base.displayLarge.copy(fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.025).em),
        displayMedium = base.displayMedium.copy(fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.02).em),
        displaySmall = base.displaySmall.copy(fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.015).em),
        headlineLarge = base.headlineLarge.copy(fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.015).em),
        headlineMedium = base.headlineMedium.copy(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em),
        headlineSmall = base.headlineSmall.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.005).em),
        titleLarge = base.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.em),
        titleMedium = base.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.009.em),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.009.em),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.017.em),
    )
}

/** Corner radii, read from the same `dimens.xml` the XML shape appearances use. */
val ChayNgayDiShapes: Shapes
    @Composable get() = Shapes(
        extraSmall = RoundedCornerShape(dimensionResource(R.dimen.corner_xs)),
        small = RoundedCornerShape(dimensionResource(R.dimen.corner_sm)),
        medium = RoundedCornerShape(dimensionResource(R.dimen.corner_md)),
        large = RoundedCornerShape(dimensionResource(R.dimen.corner_lg)),
        extraLarge = RoundedCornerShape(dimensionResource(R.dimen.corner_xl)),
    )
