package com.khiemnph.domain.util

import com.khiemnph.domain.model.LocationPoint
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Renders a recorded run as GPX 1.1.
 *
 * GPX is the portability layer this app gets instead of a Strava integration: it needs no account,
 * no OAuth and no API that can be revoked, and the same file imports into Strava, Garmin Connect,
 * Runalyze and intervals.icu. A tracker whose data cannot leave is a trap, and the whole product
 * position here is that the data is yours and stays on your phone.
 *
 * Deliberately in `:domain` and returning a String rather than writing a file, so the format is
 * pure logic with no Android, no streams and no permissions in the way of testing it.
 */
object GpxWriter {

    private const val DEFAULT_FILE_NAME = "run"

    /**
     * Coordinates are formatted through [Locale.ROOT], never the default locale.
     *
     * The app's primary language formats decimals with a comma, so a device set to Vietnamese would
     * otherwise emit `lat="21,0278"`, which is not a number to any GPX reader. The failure would
     * also be invisible to anyone testing in English.
     */
    private fun Double.asCoordinate(): String = String.format(Locale.ROOT, "%.7f", this).trimEnd('0').trimEnd('.')

    /** GPX wants UTC in ISO-8601 with a `Z`, to the second. */
    private fun Long.asGpxTime(): String =
        DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.ofEpochSecond(this / 1_000L))

    private fun String.escapedForXml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    /**
     * [name] titles the track, and [points] become one segment in run order.
     *
     * Point times come from [LocationPoint.timestamp], the wall clock, not from
     * [LocationPoint.elapsedRealtimeMillis]. Milliseconds since boot mean nothing to a consumer;
     * this is the case that keeps both clocks on a point rather than one replacing the other.
     */
    fun write(name: String, points: List<LocationPoint>): String = buildString {
        val trackName = name.ifBlank { DEFAULT_FILE_NAME }.escapedForXml()
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine(
            """<gpx version="1.1" creator="Chay Ngay Di" xmlns="http://www.topografix.com/GPX/1/1">""",
        )
        points.firstOrNull()?.let { appendLine("  <metadata><time>${it.timestamp.asGpxTime()}</time></metadata>") }
        appendLine("  <trk>")
        appendLine("    <name>$trackName</name>")
        appendLine("    <trkseg>")
        points.forEach { point ->
            appendLine(
                "      <trkpt lat=\"${point.latitude.asCoordinate()}\" lon=\"${point.longitude.asCoordinate()}\">" +
                    "<time>${point.timestamp.asGpxTime()}</time></trkpt>",
            )
        }
        appendLine("    </trkseg>")
        appendLine("  </trk>")
        appendLine("</gpx>")
    }

    /**
     * A filename to offer in the save dialog.
     *
     * Anything outside letters, digits, dash and underscore becomes an underscore, because the user
     * picks the destination and it may be a filesystem with no opinion about slashes.
     */
    fun fileNameFor(name: String): String {
        val safe = name.trim().replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_')
        return "${safe.ifBlank { DEFAULT_FILE_NAME }}.gpx"
    }
}
