package com.khiemnph.simpletracking.ui.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleBarTest {

    @Test
    fun `picks the largest round distance that fits`() {
        // At one metre per pixel, 200 m is 200 px and 500 m would overflow 250 px.
        val spec = ScaleBar.fit(metersPerPixel = 1f, maxLengthPx = 250f)!!

        assertEquals(200, spec.meters)
        assertEquals(200f, spec.lengthPx, 0.01f)
    }

    @Test
    fun `only ever picks a one two or five leading digit`() {
        val leadingDigits = generateSequence(0.02f) { it * 1.37f }
            .takeWhile { it < 200f }
            .mapNotNull { ScaleBar.fit(metersPerPixel = it, maxLengthPx = 300f) }
            .map { spec -> spec.meters.toString().trimEnd('0').toInt() }
            .toSet()

        assertEquals(setOf(1, 2, 5), leadingDigits)
    }

    @Test
    fun `scales down for a tightly zoomed route`() {
        // Ten centimetres per pixel: 20 m is 200 px, 50 m would overflow.
        val spec = ScaleBar.fit(metersPerPixel = 0.1f, maxLengthPx = 250f)!!

        assertEquals(20, spec.meters)
    }

    @Test
    fun `scales up into kilometres for a long route`() {
        val spec = ScaleBar.fit(metersPerPixel = 20f, maxLengthPx = 250f)!!

        assertEquals(5000, spec.meters)
        assertEquals(250f, spec.lengthPx, 0.01f)
    }

    @Test
    fun `returns null when even the smallest bar would overflow`() {
        assertNull(ScaleBar.fit(metersPerPixel = 0.0001f, maxLengthPx = 50f))
    }

    @Test
    fun `returns null for a non-positive scale`() {
        assertNull(ScaleBar.fit(metersPerPixel = 0f, maxLengthPx = 250f))
        assertNull(ScaleBar.fit(metersPerPixel = -1f, maxLengthPx = 250f))
    }

    @Test
    fun `returns null for a non-positive width`() {
        assertNull(ScaleBar.fit(metersPerPixel = 1f, maxLengthPx = 0f))
    }

    @Test
    fun `the bar never exceeds the space it was given`() {
        generateSequence(0.05f) { it * 1.19f }
            .takeWhile { it < 500f }
            .forEach { metersPerPixel ->
                val spec = ScaleBar.fit(metersPerPixel, maxLengthPx = 240f) ?: return@forEach
                assertTrue("$metersPerPixel overflowed", spec.lengthPx <= 240f)
            }
    }
}
