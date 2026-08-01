package com.khiemnph.simpletracking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Bricolage Grotesque, the same file and the same two optical sizes `values/type.xml` declares.
 *
 * The bundled TTF is a variable font whose default instance is "96pt ExtraBold", so weight and
 * optical size are stated on every entry. Omit them and Compose renders the whole app extra-bold at
 * a size drawn for billboards.
 *
 * Two families rather than one because optical size is not cosmetic here: the 96pt cut's tight
 * spacing falls apart at the 11sp unit labels, and the 14pt cut looks flabby at 57sp.
 */
@OptIn(ExperimentalTextApi::class)
private fun bricolage(opticalSize: Float) = FontFamily(
    listOf(400, 500, 700).map { weight ->
        Font(
            resId = R.font.bricolage_grotesque,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.Setting("opsz", opticalSize),
            ),
        )
    },
)

private val bricolageDisplay = bricolage(opticalSize = 40f)
private val bricolageText = bricolage(opticalSize = 14f)

/**
 * Tabular figures, and mandatory rather than advisory with this typeface.
 *
 * Bricolage's digits are proportional by default: its "1" is 310 units against its "0" at 608. Left
 * alone, the running timer visibly jitters every second and metric columns fail to line up.
 */
private const val TABULAR = "tnum"

@Composable
fun chayNgayDiTypography(): Typography {
    val base = MaterialTheme.typography
    return base.copy(
        displayLarge = base.displayLarge.copy(fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = -1.425.sp, fontFamily = bricolageDisplay, fontFeatureSettings = TABULAR),
        displayMedium = base.displayMedium.copy(fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = -0.9.sp, fontFamily = bricolageDisplay, fontFeatureSettings = TABULAR),
        displaySmall = base.displaySmall.copy(fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = -0.54.sp, fontFamily = bricolageDisplay, fontFeatureSettings = TABULAR),
        headlineLarge = base.headlineLarge.copy(fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = -0.48.sp, fontFamily = bricolageDisplay, fontFeatureSettings = TABULAR),
        headlineMedium = base.headlineMedium.copy(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = -0.28.sp, fontFamily = bricolageDisplay, fontFeatureSettings = TABULAR),
        headlineSmall = base.headlineSmall.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = -0.12.sp, fontFamily = bricolageDisplay, fontFeatureSettings = TABULAR),
        titleLarge = base.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp, fontFamily = bricolageText, fontFeatureSettings = TABULAR),
        titleMedium = base.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.144.sp, fontFamily = bricolageText, fontFeatureSettings = TABULAR),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.144.sp, fontFamily = bricolageText, fontFeatureSettings = TABULAR),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.238.sp, fontFamily = bricolageText, fontFeatureSettings = TABULAR),
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
