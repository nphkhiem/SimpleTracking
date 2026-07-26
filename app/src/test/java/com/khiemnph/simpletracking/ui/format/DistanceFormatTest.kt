package com.khiemnph.simpletracking.ui.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceFormatTest {

    @Test
    fun `formats metres as kilometres to two decimals`() {
        assertEquals("0.18", formatDistanceKm(180.0, Locale.US))
        assertEquals("12.50", formatDistanceKm(12_500.0, Locale.US))
    }

    @Test
    fun `keeps both decimals on a whole number of kilometres`() {
        assertEquals("5.00", formatDistanceKm(5_000.0, Locale.US))
    }

    @Test
    fun `uses the decimal separator of the locale it is given`() {
        // Vietnamese, the app's own audience, writes 12,50 rather than 12.50.
        assertEquals("12,50", formatDistanceKm(12_500.0, Locale.forLanguageTag("vi-VN")))
    }

    @Test
    fun `rounds rather than truncates`() {
        assertEquals("0.13", formatDistanceKm(125.0, Locale.US))
    }

    @Test
    fun `formats a zero distance rather than an empty string`() {
        assertEquals("0.00", formatDistanceKm(0.0, Locale.US))
    }
}
