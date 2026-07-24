package com.khiemnph.domain.util

import com.khiemnph.domain.model.LatLngPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceCalculatorTest {

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
}
