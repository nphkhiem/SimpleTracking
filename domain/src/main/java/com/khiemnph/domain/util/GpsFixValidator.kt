package com.khiemnph.domain.util

import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.RawLocationFix

/**
 * Decides whether a raw GPS fix should be accepted as a valid sample, and whether it should
 * contribute to distance accumulation.
 */
object GpsFixValidator {

    private const val MAX_ACCEPTABLE_ACCURACY_METERS = 20f

    // Above this, the implied movement is not something a person can do under their own power, so
    // the fix is bad data rather than fast data: a cold-start network fix before GPS lock, a
    // cell-tower fallback, emerging from a tunnel, or a mock-location provider. 30 m/s is ~108 km/h,
    // which clears even a fast descent on a bike.
    private const val MAX_PLAUSIBLE_SPEED_MPS = 30.0

    enum class Decision {
        REJECTED,

        /** A real sample: contributes to both the route trace and the distance total. */
        ACCEPTED,

        /**
         * A real sample for the route trace, but GPS drift rather than movement — it must not
         * contribute to distance. [DistanceCalculator.travelledDistanceMeters] is what honours
         * this; both apply [DistanceCalculator.isBelowMovementThreshold] so the two can never
         * disagree.
         */
        ACCEPTED_JITTER,
    }

    fun validate(candidate: RawLocationFix, previousAccepted: LocationPoint?): Decision {
        if (candidate.horizontalAccuracyMeters <= 0f ||
            candidate.horizontalAccuracyMeters > MAX_ACCEPTABLE_ACCURACY_METERS
        ) {
            return Decision.REJECTED
        }

        if (previousAccepted == null) {
            return Decision.ACCEPTED
        }

        val distanceMeters = DistanceCalculator.distanceBetween(
            LatLngPoint(previousAccepted.latitude, previousAccepted.longitude),
            LatLngPoint(candidate.latitude, candidate.longitude),
        )
        val elapsedMillis = candidate.elapsedRealtimeMillis - previousAccepted.elapsedRealtimeMillis

        if (isImplausiblyFast(distanceMeters, elapsedMillis)) {
            return Decision.REJECTED
        }

        return if (DistanceCalculator.isBelowMovementThreshold(distanceMeters, elapsedMillis)) {
            Decision.ACCEPTED_JITTER
        } else {
            Decision.ACCEPTED
        }
    }

    /**
     * Whether covering [distanceMeters] in [elapsedMillis] is physically implausible.
     *
     * A non-positive [elapsedMillis] means a duplicated or out-of-order timestamp: there is no time
     * to have moved in, so any hop big enough to count as movement is bad data. Below that
     * threshold the fix is harmless drift and is left to the jitter path.
     */
    private fun isImplausiblyFast(distanceMeters: Double, elapsedMillis: Long): Boolean {
        if (elapsedMillis <= 0L) {
            return !DistanceCalculator.isBelowMovementThreshold(distanceMeters, elapsedMillis)
        }
        return distanceMeters / (elapsedMillis / 1_000.0) > MAX_PLAUSIBLE_SPEED_MPS
    }
}
