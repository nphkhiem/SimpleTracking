package com.khiemnph.domain.util

import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.LocationPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes great-circle distance between coordinates using the haversine formula.
 *
 * This object also owns the threshold that decides whether a hop between two consecutive fixes
 * counts as movement at all. [GpsFixValidator] maps that same threshold onto
 * [GpsFixValidator.Decision.ACCEPTED_JITTER], so a hop can never be treated as noise when
 * classifying a fix and as movement when accumulating distance.
 */
object DistanceCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * A hop shorter than this is only real movement if enough time passed for it to be plausible;
     * otherwise it is GPS drift while stationary.
     */
    const val MOVEMENT_DISTANCE_THRESHOLD_METERS = 2.0

    /** @see MOVEMENT_DISTANCE_THRESHOLD_METERS */
    const val MOVEMENT_TIME_THRESHOLD_MILLIS = 5_000L

    fun distanceBetween(from: LatLngPoint, to: LatLngPoint): Double {
        val fromLatRad = Math.toRadians(from.latitude)
        val toLatRad = Math.toRadians(to.latitude)
        val deltaLatRad = Math.toRadians(to.latitude - from.latitude)
        val deltaLngRad = Math.toRadians(to.longitude - from.longitude)

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
            cos(fromLatRad) * cos(toLatRad) * sin(deltaLngRad / 2) * sin(deltaLngRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    fun totalDistanceMeters(points: List<LatLngPoint>): Double {
        if (points.size < 2) return 0.0
        return points.zipWithNext { current, next -> distanceBetween(current, next) }.sum()
    }

    /**
     * Distance actually travelled along [points], excluding hops that are GPS drift rather than
     * movement (see [isBelowMovementThreshold]).
     *
     * Prefer this over [totalDistanceMeters] for any distance shown to the user; reserve
     * [totalDistanceMeters] for pure route geometry, where every recorded point matters.
     *
     * The drift classification is recomputed here rather than read from storage, and that is exact:
     * fixes rejected by [GpsFixValidator] are never persisted, so a persisted point's predecessor is
     * the very fix the validator compared it against when it was recorded.
     */
    fun travelledDistanceMeters(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        return points.zipWithNext { current, next ->
            val hopMeters = distanceBetween(
                LatLngPoint(current.latitude, current.longitude),
                LatLngPoint(next.latitude, next.longitude),
            )
            if (isBelowMovementThreshold(hopMeters, next.timestamp - current.timestamp)) 0.0 else hopMeters
        }.sum()
    }

    /**
     * Whether a hop of [distanceMeters] over [elapsedMillis] is too small to be real movement. A
     * short hop is plausible when enough time has passed (a genuine slow walk or a stop); arriving
     * too soon, it is drift.
     */
    fun isBelowMovementThreshold(distanceMeters: Double, elapsedMillis: Long): Boolean =
        distanceMeters < MOVEMENT_DISTANCE_THRESHOLD_METERS && elapsedMillis < MOVEMENT_TIME_THRESHOLD_MILLIS
}
