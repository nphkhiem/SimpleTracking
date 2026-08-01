package com.khiemnph.domain.util

import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.LocationPoint

private const val METERS_PER_SPLIT = 1_000.0

/**
 * A run that ends on a kilometre mark should report a complete kilometre, not one a millionth of a
 * metre short. Summing hundreds of haversine hops leaves error far below this, and far below any
 * distance GPS can resolve, so this only ever absorbs arithmetic noise.
 */
private const val COMPLETE_SPLIT_EPSILON_METERS = 0.01

/**
 * Breaks a recorded run into per-kilometre splits.
 *
 * Uses the same drift rule as [DistanceCalculator.travelledDistanceMeters], deliberately: splits
 * that do not add up to the distance shown at the top of the screen are worse than no splits at
 * all. Stationary wobble is excluded from both, so a run that pauses at a crossing does not
 * accumulate a phantom kilometre.
 *
 * A hop that crosses a kilometre boundary is divided in proportion to the distance either side of
 * it, rather than assigned wholesale to one split. At a one-second sampling rate a hop is a few
 * metres, so this is a small correction, but it keeps the boundary honest instead of letting split
 * times drift by whole seconds as the run goes on.
 */
object SplitsCalculator {

    fun splitsFor(points: List<LocationPoint>, splitMeters: Double = METERS_PER_SPLIT): List<Split> {
        if (points.size < 2) return emptyList()

        val splits = mutableListOf<Split>()
        var distanceInSplit = 0.0
        var durationInSplit = 0L

        points.zipWithNext { current, next ->
            val hopMillis = next.elapsedRealtimeMillis - current.elapsedRealtimeMillis
            val hopMeters = DistanceCalculator.distanceBetween(
                LatLngPoint(current.latitude, current.longitude),
                LatLngPoint(next.latitude, next.longitude),
            ).takeUnless { DistanceCalculator.isBelowMovementThreshold(it, hopMillis) } ?: 0.0

            if (hopMeters <= 0.0) {
                // Time still passes while standing still, and it belongs to the split in progress.
                durationInSplit += hopMillis
                return@zipWithNext
            }

            var remainingMeters = hopMeters
            var remainingMillis = hopMillis
            while (distanceInSplit + remainingMeters >= splitMeters) {
                val metersToBoundary = splitMeters - distanceInSplit
                val millisToBoundary = (remainingMillis * (metersToBoundary / remainingMeters)).toLong()

                splits += Split(
                    index = splits.size + 1,
                    distanceMeters = splitMeters,
                    durationMillis = durationInSplit + millisToBoundary,
                    isPartial = false,
                )

                remainingMeters -= metersToBoundary
                remainingMillis -= millisToBoundary
                distanceInSplit = 0.0
                durationInSplit = 0L
            }

            distanceInSplit += remainingMeters
            durationInSplit += remainingMillis
        }

        if (distanceInSplit > 0.0) {
            val completesTheSplit = distanceInSplit >= splitMeters - COMPLETE_SPLIT_EPSILON_METERS
            splits += Split(
                index = splits.size + 1,
                distanceMeters = if (completesTheSplit) splitMeters else distanceInSplit,
                durationMillis = durationInSplit,
                isPartial = !completesTheSplit,
            )
        }

        return splits
    }
}
