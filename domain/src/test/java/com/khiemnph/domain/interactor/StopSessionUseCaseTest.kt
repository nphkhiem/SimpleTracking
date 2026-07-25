package com.khiemnph.domain.interactor

import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.util.DistanceCalculator
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

        val summary = useCase(sessionId, thumbnailPath = null)

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

        val summary = useCase(sessionId, thumbnailPath = null)

        assertEquals(0.0, summary.distanceMeters, 0.0001)
    }

    @Test
    fun givenMultipleAcceptedSpeedSamples_whenStop_thenAverageSpeedEqualsArithmeticMeanNotTotalDistanceOverDuration() = runTest {
        val sessionId = repository.startSession()
        // Speeds chosen so the arithmetic mean (2.0) differs sharply from distance/duration,
        // which would be a tiny fraction of a m/s over this short synthetic path/time span.
        val points = listOf(
            point(10.7626, 106.6602, 1_000L, 1f),
            point(10.7626, 106.6602, 2_000L, 2f),
            point(10.7626, 106.6602, 3_000L, 3f),
        )
        repository.seedPoints(sessionId, points)

        val summary = useCase(sessionId, thumbnailPath = null)

        assertEquals(2.0f, summary.averageSpeedMps, 0.0001f)
    }

    @Test
    fun givenNoPersistedPoints_whenStop_thenFinalDistanceAndAverageSpeedAreZero() = runTest {
        val sessionId = repository.startSession()

        val summary = useCase(sessionId, thumbnailPath = null)

        assertEquals(0.0, summary.distanceMeters, 0.0001)
        assertEquals(0f, summary.averageSpeedMps, 0.0001f)
    }

    @Test
    fun givenThumbnailPath_whenStop_thenSummaryCarriesThumbnailPath() = runTest {
        val sessionId = repository.startSession()

        val summary = useCase(sessionId, thumbnailPath = "/path/to/thumb.png")

        assertEquals("/path/to/thumb.png", summary.thumbnailPath)
    }
}
