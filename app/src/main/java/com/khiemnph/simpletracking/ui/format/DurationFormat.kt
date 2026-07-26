package com.khiemnph.simpletracking.ui.format

import java.util.Locale

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L

/**
 * Elapsed time as `m:ss`, widening to `h:mm:ss` once a session reaches an hour.
 *
 * The leading unit is deliberately not zero-padded, matching the usual stopwatch convention and
 * Android's own `DateUtils.formatElapsedTime`.
 *
 * [locale] defaults to the device's rather than being pinned, so digits render in the numbering
 * system the reader actually uses. Tests pass an explicit locale so their expectations do not
 * depend on the machine they run on.
 *
 * A negative duration is clamped to zero. It is reachable from a backwards clock change, and used
 * to render as text like `-0:-15`.
 */
fun formatDuration(durationMillis: Long, locale: Locale = Locale.getDefault()): String {
    val totalSeconds = (durationMillis / MILLIS_PER_SECOND).coerceAtLeast(0L)
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(locale, "%d:%02d", minutes, seconds)
    }
}
