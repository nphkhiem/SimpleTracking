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
    private const val JITTER_DISTANCE_THRESHOLD_METERS = 2.0

    // A fix under the jitter distance threshold is only plausible as a genuine slow/stopped
    // reading if at least this much time has passed since the previous accepted fix; otherwise
    // it's more likely GPS noise (drift while stationary) than real movement.
    private const val JITTER_TIME_THRESHOLD_MILLIS = 5_000L

    enum class Decision {
        REJECTED,
        ACCEPTED,
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

        val isImplausiblyClose = distanceMeters < JITTER_DISTANCE_THRESHOLD_METERS
        val isTooSoonForLegitimateStop = elapsedMillis < JITTER_TIME_THRESHOLD_MILLIS

        return if (isImplausiblyClose && isTooSoonForLegitimateStop) {
            Decision.ACCEPTED_JITTER
        } else {
            Decision.ACCEPTED
        }
    }
}
