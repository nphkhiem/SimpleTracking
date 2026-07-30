package com.khiemnph.domain.util

/**
 * One kilometre of a run, or the leftover distance at the end.
 *
 * [isPartial] matters for display: a 400 m closing split is not a slow kilometre, and showing its
 * pace next to complete ones without saying so would make every run look like it fell apart at the
 * finish.
 */
data class Split(
    val index: Int,
    val distanceMeters: Double,
    val durationMillis: Long,
    val isPartial: Boolean,
) {
    /**
     * Seconds per kilometre, extrapolated for a partial split so it is comparable with the rest.
     * A split that covered no ground reports zero rather than infinity.
     */
    val paceSecondsPerKm: Double
        get() = if (distanceMeters <= 0.0) 0.0 else (durationMillis / 1_000.0) / (distanceMeters / 1_000.0)
}
