package com.khiemnph.domain.util

import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceCalculatorTest {

    /** ~0.5 m of latitude — below the movement threshold. */
    private val jitterLatitudeStep = 0.0000045

    /** ~11 m of latitude — unambiguously real movement. */
    private val movingLatitudeStep = 0.0001

    private fun point(latitude: Double, timestamp: Long) = LocationPoint(
        sessionId = "session-1",
        latitude = latitude,
        longitude = 106.6602,
        timestamp = timestamp,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = 0f,
    )

    @Test
    fun givenTwoKnownCoordinates_whenCalculateDistance_thenMatchesExpectedHaversineValueWithinTolerance() {
        // Eiffel Tower to Arc de Triomphe, Paris — well-known straight-line distance ~1.71 km.
        val eiffelTower = LatLngPoint(latitude = 48.8584, longitude = 2.2945)
        val arcDeTriomphe = LatLngPoint(latitude = 48.8738, longitude = 2.2950)

        val distanceMeters = DistanceCalculator.distanceBetween(eiffelTower, arcDeTriomphe)

        assertEquals(1713.0, distanceMeters, 50.0)
    }

    @Test
    fun givenSamePointTwice_whenCalculateDistance_thenDistanceIsZero() {
        val point = LatLngPoint(latitude = 10.762622, longitude = 106.660172)

        val distanceMeters = DistanceCalculator.distanceBetween(point, point)

        assertEquals(0.0, distanceMeters, 0.0001)
    }

    @Test
    fun givenOrderedRouteOfThreePoints_whenSumDistance_thenEqualsSumOfConsecutiveSegments() {
        val a = LatLngPoint(latitude = 10.7626, longitude = 106.6602)
        val b = LatLngPoint(latitude = 10.7630, longitude = 106.6610)
        val c = LatLngPoint(latitude = 10.7640, longitude = 106.6625)

        val expected = DistanceCalculator.distanceBetween(a, b) + DistanceCalculator.distanceBetween(b, c)
        val total = DistanceCalculator.totalDistanceMeters(listOf(a, b, c))

        assertEquals(expected, total, 0.0001)
    }

    @Test
    fun givenEmptyRoute_whenSumDistance_thenDistanceIsZero() {
        assertEquals(0.0, DistanceCalculator.totalDistanceMeters(emptyList()), 0.0001)
    }

    @Test
    fun givenSinglePointRoute_whenSumDistance_thenDistanceIsZero() {
        val a = LatLngPoint(latitude = 10.7626, longitude = 106.6602)

        assertEquals(0.0, DistanceCalculator.totalDistanceMeters(listOf(a)), 0.0001)
    }

    @Test
    fun givenStationaryJitterPoints_whenSumTravelledDistance_thenDistanceIsZero() {
        // A phone sitting still: sub-metre wobble arriving every second. Summing these hops is what
        // logged 0.70 km while standing still for 42 s.
        val points = (0..5).map { index ->
            point(latitude = 10.7626 + jitterLatitudeStep * index, timestamp = 10_000L + index * 1_000L)
        }

        assertEquals(0.0, DistanceCalculator.travelledDistanceMeters(points), 0.0001)
    }

    @Test
    fun givenGenuineMovement_whenSumTravelledDistance_thenEveryHopCounted() {
        val points = (0..3).map { index ->
            point(latitude = 10.7626 + movingLatitudeStep * index, timestamp = 10_000L + index * 1_000L)
        }

        val expected = DistanceCalculator.totalDistanceMeters(
            points.map { LatLngPoint(it.latitude, it.longitude) },
        )

        assertEquals(expected, DistanceCalculator.travelledDistanceMeters(points), 0.0001)
    }

    @Test
    fun givenMovementFollowedByStationaryJitter_whenSumTravelledDistance_thenOnlyRealHopsCounted() {
        val moving = (0..2).map { index ->
            point(latitude = 10.7626 + movingLatitudeStep * index, timestamp = 10_000L + index * 1_000L)
        }
        val restingStart = moving.last()
        val resting = (1..3).map { index ->
            point(
                latitude = restingStart.latitude + jitterLatitudeStep * index,
                timestamp = restingStart.timestamp + index * 1_000L,
            )
        }

        val expected = DistanceCalculator.totalDistanceMeters(
            moving.map { LatLngPoint(it.latitude, it.longitude) },
        )

        assertEquals(expected, DistanceCalculator.travelledDistanceMeters(moving + resting), 0.0001)
    }

    @Test
    fun givenPointsFarApartButLongElapsedTime_whenSumTravelledDistance_thenHopCounted() {
        // A genuine slow walk: below the jitter distance threshold is only noise when it also
        // arrives too soon. 30 s apart, it is real.
        val points = listOf(
            point(latitude = 10.7626, timestamp = 10_000L),
            point(latitude = 10.7626 + jitterLatitudeStep, timestamp = 40_000L),
        )

        val expected = DistanceCalculator.distanceBetween(
            LatLngPoint(points[0].latitude, points[0].longitude),
            LatLngPoint(points[1].latitude, points[1].longitude),
        )

        assertEquals(expected, DistanceCalculator.travelledDistanceMeters(points), 0.0001)
    }

    @Test
    fun givenFewerThanTwoPoints_whenSumTravelledDistance_thenDistanceIsZero() {
        assertEquals(0.0, DistanceCalculator.travelledDistanceMeters(emptyList()), 0.0001)
        assertEquals(
            0.0,
            DistanceCalculator.travelledDistanceMeters(listOf(point(10.7626, 10_000L))),
            0.0001,
        )
    }
}
