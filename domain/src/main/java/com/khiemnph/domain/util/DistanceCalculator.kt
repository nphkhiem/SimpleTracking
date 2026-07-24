package com.khiemnph.domain.util

import com.khiemnph.domain.model.LatLngPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes great-circle distance between coordinates using the haversine formula.
 */
object DistanceCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

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
}
