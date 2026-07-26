package com.khiemnph.simpletracking.ui.record

import org.junit.Assert.assertEquals
import org.junit.Test

class PaceFormatTest {

    @Test
    fun givenARunningSpeed_whenFormatted_thenItReadsAsMinutesPerKilometre() {
        // 3.1 m/s is a hair under 5:23 per km.
        assertEquals("5:23", formatPaceMinPerKm(3.1f))
    }

    @Test
    fun givenAFasterSpeed_whenFormatted_thenThePaceIsLower() {
        assertEquals("4:00", formatPaceMinPerKm(1_000f / 240f))
    }

    @Test
    fun givenSecondsUnderTen_whenFormatted_thenTheyArePadded() {
        assertEquals("5:05", formatPaceMinPerKm(1_000f / 305f))
    }

    /** Standing still is not an infinitely slow pace, it is no pace. */
    @Test
    fun givenNoMovement_whenFormatted_thenPaceIsBlankRatherThanInfinite() {
        assertEquals("--:--", formatPaceMinPerKm(0f))
    }

    @Test
    fun givenANegativeSpeed_whenFormatted_thenPaceIsBlank() {
        assertEquals("--:--", formatPaceMinPerKm(-1f))
    }

    /**
     * A crawl would otherwise render as something like "312:47", which is noise rather than
     * information and would blow the width of the readout.
     */
    @Test
    fun givenAnImplausiblySlowCrawl_whenFormatted_thenPaceIsBlankRatherThanAbsurd() {
        assertEquals("--:--", formatPaceMinPerKm(0.05f))
    }
}
