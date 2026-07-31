package com.khiemnph.simpletracking.ui.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.simpletracking.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

/** Material's minimum, and the brief's: nothing interactive smaller than this. */
private const val MINIMUM_TARGET_DP = 48

/**
 * Measures the Record screen's controls rather than reading the layout file.
 *
 * Reading the file would pass on a declared 48dp that padding or a wrapping parent then shrinks.
 * This asks the views how big they actually came out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecordTouchTargetTest {

    private fun measuredRoot(): View {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.Theme_ChayNgayDi,
        )
        val root = LayoutInflater.from(context).inflate(R.layout.fragment_record, null)
        // The fallback ComposeView needs a window recomposer to measure, and this test is about
        // the View controls. Detaching it keeps the measurement honest without standing up an
        // Activity: it is a full-bleed overlay and constrains nothing.
        (root as ViewGroup).removeView(root.findViewById(R.id.record_route_fallback))
        val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY)
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        return root
    }

    private fun dp(pixels: Int, root: View): Int =
        (pixels / root.resources.displayMetrics.density).roundToInt()

    @Test
    fun `every control on Record is at least 48dp square`() {
        val root = measuredRoot()
        val controls = listOf(
            "close" to R.id.record_back_button,
            "pause or resume" to R.id.record_pause_resume_button,
            "stop" to R.id.record_stop_button,
        )

        val tooSmall = controls.mapNotNull { (name, id) ->
            val view = requireNotNull(root.findViewById<View>(id)) { "$name is missing from the layout" }
            val width = dp(view.measuredWidth, root)
            val height = dp(view.measuredHeight, root)
            if (width < MINIMUM_TARGET_DP || height < MINIMUM_TARGET_DP) {
                "$name is ${width}x${height}dp, needs ${MINIMUM_TARGET_DP}dp"
            } else {
                null
            }
        }

        assertTrue(tooSmall.joinToString("\n"), tooSmall.isEmpty())
    }

    @Test
    fun `the sheet advertises no gesture it does not support`() {
        // Asserted against the layout source rather than the view tree, because the handle has no
        // id to look up: it is decoration. The sheet is pinned and non-draggable, so a handle
        // promises a drag that never happens.
        val layout = java.io.File("src/main/res/layout/fragment_record.xml").readText()

        assertTrue(
            "a drag handle on a non-draggable sheet is a false affordance",
            !layout.contains("bg_sheet_handle"),
        )
    }

    @Test
    fun `the layout measures without a zero-sized control`() {
        val root = measuredRoot()
        val sheet = requireNotNull(root.findViewById<ViewGroup>(R.id.record_bottom_sheet))

        assertTrue("the sheet collapsed to nothing", sheet.measuredHeight > 0)
    }

    /**
     * The landscape variant must carry every id the portrait one does, or the Fragment's view
     * binding fails at runtime the moment someone rotates. Comparing the two files catches an id
     * renamed in one and not the other, which is the way this actually breaks.
     */
    @Test
    fun `the landscape layout declares every id the portrait layout does`() {
        fun idsIn(path: String): Set<String> =
            Regex("""android:id="@\+?id/(\w+)"""")
                .findAll(java.io.File(path).readText())
                .map { it.groupValues[1] }
                .toSet()

        val portrait = idsIn("src/main/res/layout/fragment_record.xml")
        val landscape = idsIn("src/main/res/layout-land/fragment_record.xml")

        assertTrue("landscape is missing ${portrait - landscape}", (portrait - landscape).isEmpty())
    }

    /**
     * The point of the landscape variant: the sheet becomes a side rail so the map keeps most of
     * the screen. Laid out as a bottom strip it took roughly 62 percent of the height and left a
     * letterbox of map, which is the regression this guards.
     */
    @Test
    @Config(sdk = [33], qualifiers = "land")
    fun `in landscape the sheet is a side rail, not a strip across the bottom`() {
        val root = measuredRoot()
        val sheet = requireNotNull(root.findViewById<ViewGroup>(R.id.record_bottom_sheet))

        assertTrue(
            "the sheet spans the width, so it is still a bottom strip",
            sheet.measuredWidth < root.measuredWidth / 2,
        )
        assertTrue(
            "the rail should run the full height",
            sheet.measuredHeight > root.measuredHeight / 2,
        )
    }

    @Test
    fun `in portrait the sheet spans the width`() {
        val root = measuredRoot()
        val sheet = requireNotNull(root.findViewById<ViewGroup>(R.id.record_bottom_sheet))

        assertTrue(sheet.measuredWidth == root.measuredWidth)
    }
}
