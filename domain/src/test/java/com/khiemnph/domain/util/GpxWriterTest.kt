package com.khiemnph.domain.util

import com.khiemnph.domain.model.LocationPoint
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxWriterTest {

    private val defaultLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(defaultLocale)

    private fun point(latitude: Double, longitude: Double, atMillis: Long) = LocationPoint(
        sessionId = "s",
        latitude = latitude,
        longitude = longitude,
        timestamp = atMillis,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = 3f,
    )

    // 2026-08-02T03:14:15Z
    private val startMillis = 1_785_640_455_000L

    private val route = listOf(
        point(21.0278, 105.8342, startMillis),
        point(21.0288, 105.8352, startMillis + 5_000L),
    )

    @Test
    fun `writes a well formed GPX 1_1 document`() {
        val gpx = GpxWriter.write("Morning run", route)

        assertTrue(gpx.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue(gpx.contains("""<gpx version="1.1""""))
        assertTrue(gpx.contains("""xmlns="http://www.topografix.com/GPX/1/1""""))
        assertTrue(gpx.trim().endsWith("</gpx>"))
    }

    @Test
    fun `every point becomes a track point`() {
        val gpx = GpxWriter.write("Morning run", route)

        assertEquals(2, Regex("<trkpt ").findAll(gpx).count())
        assertTrue(gpx.contains("""<trkpt lat="21.0278" lon="105.8342">"""))
    }

    @Test
    fun `times are UTC and come from the wall clock`() {
        // Not the monotonic clock: milliseconds since boot mean nothing to Strava or Garmin. This
        // is the reason both clocks are kept on a point rather than one replacing the other.
        val gpx = GpxWriter.write("Morning run", route)

        assertTrue(gpx, gpx.contains("<time>2026-08-02T03:14:15Z</time>"))
        assertTrue(gpx, gpx.contains("<time>2026-08-02T03:14:20Z</time>"))
    }

    @Test
    fun `coordinates use a point decimal separator whatever the device locale`() {
        // The app's primary locale formats decimals with a comma. lat="21,0278" is not a number to
        // any GPX reader, and this would be invisible on an English test device.
        Locale.setDefault(Locale.forLanguageTag("vi-VN"))

        val gpx = GpxWriter.write("Morning run", route)

        assertTrue(gpx, gpx.contains("""lat="21.0278""""))
        assertFalse("a comma separator would break every consumer", gpx.contains("""lat="21,"""))
    }

    @Test
    fun `a name containing markup is escaped rather than injected`() {
        val gpx = GpxWriter.write("""Ăn & <chạy> "sáng"""", route)

        assertTrue(gpx, gpx.contains("<name>Ăn &amp; &lt;chạy&gt; &quot;sáng&quot;</name>"))
        assertFalse(gpx.contains("<chạy>"))
    }

    @Test
    fun `a run with no points still produces a valid document`() {
        val gpx = GpxWriter.write("Empty", emptyList())

        assertTrue(gpx.contains("<trkseg>"))
        assertEquals(0, Regex("<trkpt ").findAll(gpx).count())
        assertTrue(gpx.trim().endsWith("</gpx>"))
    }

    @Test
    fun `a suggested filename is safe for any file system`() {
        assertEquals("Chay_sang_1_8.gpx", GpxWriter.fileNameFor("Chay sang 1/8"))
        assertEquals("run.gpx", GpxWriter.fileNameFor("   "))
    }
}
