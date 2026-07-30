package com.khiemnph.simpletracking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
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
 * Letter spacing is expressed in `sp`, not the `em` that `android:letterSpacing` uses, even though
 * that means the XML figure does not carry across unchanged. Material's own styles use `sp`, and
 * Compose cannot interpolate between the two units: it throws "Cannot perform operation for Em and
 * Sp". Any component that animates between two type styles hits that, and `OutlinedTextField`'s
 * floating label animates between `bodyLarge` and `bodySmall`, so every labelled text field in the
 * app crashed until this was made consistent. Each value below is its `em` equivalent multiplied by
 * its own font size, so nothing renders differently.
 */
@Composable
fun chayNgayDiTypography(): Typography {
    val base = MaterialTheme.typography
    return base.copy(
        displayLarge = base.displayLarge.copy(fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = -1.425.sp),
        displayMedium = base.displayMedium.copy(fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = -0.9.sp),
        displaySmall = base.displaySmall.copy(fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = -0.54.sp),
        headlineLarge = base.headlineLarge.copy(fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = -0.48.sp),
        headlineMedium = base.headlineMedium.copy(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = -0.28.sp),
        headlineSmall = base.headlineSmall.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = -0.12.sp),
        titleLarge = base.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        titleMedium = base.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.144.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.144.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.238.sp),
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
