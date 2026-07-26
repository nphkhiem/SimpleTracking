package com.khiemnph.domain.interactor

import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.util.DistanceCalculator
import com.khiemnph.domain.util.RoutePolyline
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StopSessionUseCaseTest {

    private val repository = MockedSessionRepository()
    private val useCase = StopSessionUseCase(repository)

    private fun point(lat: Double, lng: Double, timestamp: Long, speed: Float) = LocationPoint(
        sessionId = "session-1",
        latitude = lat,
        longitude = lng,
        timestamp = timestamp,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = speed,
    )

    @Test
    fun givenPersistedPointsFormingAKnownPath_whenStop_thenFinalDistanceEqualsSumOfHaversineSegments() = runTest {
        val sessionId = repository.startSession()
        val points = listOf(
            point(10.7626, 106.6602, 1_000L, 1f),
            point(10.7630, 106.6610, 2_000L, 2f),
            point(10.7640, 106.6625, 3_000L, 3f),
        )
        repository.seedPoints(sessionId, points)
        val expectedDistance = DistanceCalculator.totalDistanceMeters(
            points.map { LatLngPoint(it.latitude, it.longitude) },
        )

        val summary = useCase(sessionId)

        assertEquals(expectedDistance, summary.distanceMeters, 0.0001)
    }

    @Test
    fun givenStationaryJitterPoints_whenStop_thenFinalDistanceExcludesDrift() = runTest {
        val sessionId = repository.startSession()
        // A phone sitting still: sub-metre wobble arriving every second.
        val points = (0..5).map { index ->
            point(10.7626 + 0.0000045 * index, 106.6602, 1_000L + index * 1_000L, 0.1f)
        }
        repository.seedPoints(sessionId, points)

        val summary = useCase(sessionId)

        assertEquals(0.0, summary.distanceMeters, 0.0001)
    }

    @Test
    fun givenSamplesReportingSpeedButNoActualMovement_whenStop_thenAverageSpeedIsZero() = runTest {
        val sessionId = repository.startSession()
        // Identical coordinates, so nothing was travelled, while the provider still reported a
        // speed for every sample. Averaging those samples would claim 2 m/s over a 0 m route: the
        // exact contradiction that made a history row read "0.70 km, 0:42, 0.2 km/h avg".
        val points = listOf(
            point(10.7626, 106.6602, 1_000L, 1f),
            point(10.7626, 106.6602, 2_000L, 2f),
            point(10.7626, 106.6602, 3_000L, 3f),
        )
        repository.seedPoints(sessionId, points)

        val summary = useCase(sessionId)

        assertEquals(0.0, summary.distanceMeters, 0.0001)
        assertEquals(0f, summary.averageSpeedMps, 0.0001f)
    }

    @Test
    fun givenNoPersistedPoints_whenStop_thenFinalDistanceAndAverageSpeedAreZero() = runTest {
        val sessionId = repository.startSession()

        val summary = useCase(sessionId)

        assertEquals(0.0, summary.distanceMeters, 0.0001)
        assertEquals(0f, summary.averageSpeedMps, 0.0001f)
    }

    @Test
    fun givenRecordedPoints_whenStop_thenSummaryCarriesTheRoutesShape() = runTest {
        val sessionId = repository.startSession()
        val points = listOf(
            point(10.7626, 106.6602, 1_000L, 1f),
            point(10.7630, 106.6610, 2_000L, 2f),
            point(10.7640, 106.6625, 3_000L, 3f),
        )
        repository.seedPoints(sessionId, points)

        val summary = useCase(sessionId)

        // Derived from the recorded points, not supplied by the caller: no argument to get wrong.
        val decoded = RoutePolyline.decode(summary.routePolyline!!)
        assertEquals(points.size, decoded.size)
        assertEquals(points.first().latitude, decoded.first().latitude, RoutePolyline.PRECISION_DEGREES)
    }

    @Test
    fun givenASessionWithNoRecordedPoints_whenStop_thenThereIsNoRouteToDraw() = runTest {
        val sessionId = repository.startSession()

        assertEquals(null, useCase(sessionId).routePolyline)
    }
}
