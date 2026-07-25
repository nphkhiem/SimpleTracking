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
        val elapsedMillis = candidate.timestamp - previousAccepted.timestamp

        return if (DistanceCalculator.isBelowMovementThreshold(distanceMeters, elapsedMillis)) {
            Decision.ACCEPTED_JITTER
        } else {
            Decision.ACCEPTED
        }
    }
}
