package com.khiemnph.simpletracking.ui.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun `formats under a minute with a zero minute field`() {
        assertEquals("0:07", formatDuration(7_000L, Locale.US))
    }

    @Test
    fun `formats minutes and seconds without padding the leading unit`() {
        assertEquals("1:23", formatDuration(83_000L, Locale.US))
        assertEquals("28:14", formatDuration(1_694_000L, Locale.US))
    }

    @Test
    fun `adds an hours field only once a session reaches an hour`() {
        assertEquals("59:59", formatDuration(3_599_000L, Locale.US))
        assertEquals("1:00:00", formatDuration(3_600_000L, Locale.US))
        assertEquals("1:01:01", formatDuration(3_661_000L, Locale.US))
    }

    @Test
    fun `truncates sub-second remainders rather than rounding up`() {
        assertEquals("0:01", formatDuration(1_999L, Locale.US))
    }

    @Test
    fun `clamps a negative duration to zero instead of rendering a negative clock`() {
        assertEquals("0:00", formatDuration(-15_000L, Locale.US))
    }

    @Test
    fun `renders digits in the locale it is given`() {
        // Locale decides digit shapes, so the formatter must not silently impose its own.
        val arabicIndic = Locale.forLanguageTag("ar-EG-u-nu-arab")

        assertEquals("١:٢٣", formatDuration(83_000L, arabicIndic))
    }
}
