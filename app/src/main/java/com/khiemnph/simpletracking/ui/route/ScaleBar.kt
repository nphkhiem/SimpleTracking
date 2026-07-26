package com.khiemnph.simpletracking.ui.route

/** A scale bar of [meters] on the ground, drawn [lengthPx] long. */
data class ScaleBarSpec(
    val meters: Int,
    val lengthPx: Float,
)

/**
 * Chooses the scale bar for a view whose zoom is decided by its content rather than by the user.
 *
 * A route view that fits itself to whatever has been recorded so far has no fixed zoom, so the
 * only way to read size off it is a bar labelled with a real distance. The label is always a 1, 2
 * or 5 followed by zeros, which is the convention every map uses: those are the values a reader
 * can halve or double in their head to estimate anything else on screen.
 */
object ScaleBar {

    private val LEADING_DIGITS = listOf(1, 2, 5)
    private const val SMALLEST_BAR_METERS = 1.0

    fun fit(metersPerPixel: Float, maxLengthPx: Float): ScaleBarSpec? {
        if (metersPerPixel <= 0f || maxLengthPx <= 0f) return null

        val maxMeters = maxLengthPx * metersPerPixel
        if (maxMeters < SMALLEST_BAR_METERS) return null

        val meters = generateSequence(SMALLEST_BAR_METERS) { it * 10 }
            .takeWhile { it <= maxMeters }
            .flatMap { decade -> LEADING_DIGITS.asSequence().map { it * decade } }
            .filter { it <= maxMeters }
            .last()

        return ScaleBarSpec(meters = meters.toInt(), lengthPx = (meters / metersPerPixel).toFloat())
    }
}
