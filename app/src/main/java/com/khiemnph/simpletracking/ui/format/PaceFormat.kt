package com.khiemnph.simpletracking.ui.format

import java.util.Locale
import kotlin.math.roundToInt

private const val SECONDS_PER_MINUTE = 60
private const val METERS_PER_KILOMETER = 1_000f

/**
 * Below this a reading is a crawl rather than a run, and its pace is not worth showing: at 0.05 m/s
 * the honest answer is "312:47 per km", which is noise and would blow the width of the readout.
 */
private const val SLOWEST_MEANINGFUL_SPEED_MPS = 0.5f

/** Shown instead of a pace when there is not enough movement to compute one. */
private const val NO_PACE = "--:--"

/**
 * Formats speed as pace in minutes per kilometre.
 *
 * Pace is the unit runners actually use and talk in; km/h is a car's unit. Every screen that states
 * how fast a run was goes through here, so the app cannot end up quoting one unit in a list and
 * another on the detail behind it, which it did until the Runs row moved off km/h.
 */
fun formatPaceMinPerKm(speedMps: Float): String {
    if (speedMps < SLOWEST_MEANINGFUL_SPEED_MPS) return NO_PACE

    // Rounded, not truncated: 322.6 s per km is 5:23, and truncating would under-report
    // every pace by up to a second.
    val secondsPerKm = (METERS_PER_KILOMETER / speedMps).roundToInt()
    return String.format(
        Locale.getDefault(),
        "%d:%02d",
        secondsPerKm / SECONDS_PER_MINUTE,
        secondsPerKm % SECONDS_PER_MINUTE,
    )
}
