package com.khiemnph.domain.interactor

import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.RawLocationFix
import com.khiemnph.domain.util.DistanceCalculator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordLocationFixUseCaseTest {

    private val repository = MockedSessionRepository()
    private val useCase = RecordLocationFixUseCase(repository)

    private fun rawFix(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        timestamp: Long,
        horizontalAccuracyMeters: Float = 5f,
        speedMetersPerSec: Float? = null,
    ) = RawLocationFix(sessionId, latitude, longitude, timestamp, horizontalAccuracyMeters, speedMetersPerSec)

    @Test
    fun givenFirstFixWithNoReportedSpeed_whenRecord_thenFallbackSpeedIsZero() = runTest {
        val sessionId = repository.startSession()
        val fix = rawFix(sessionId, 10.7626, 106.6602, 1_000L, speedMetersPerSec = null)

        useCase(fix)

        val persisted = repository.getPointsForSession(sessionId)
        assertEquals(1, persisted.size)
        assertEquals(0f, persisted.first().speedMetersPerSec, 0.0001f)
    }

    @Test
    fun givenSubsequentFixWithNoReportedSpeed_whenRecord_thenFallbackSpeedIsDistanceOverTime() = runTest {
        val sessionId = repository.startSession()
        useCase(rawFix(sessionId, 10.7626, 106.6602, 1_000L, speedMetersPerSec = 0f))
        val second = rawFix(sessionId, 10.7626, 106.6700, timestamp = 11_000L, speedMetersPerSec = null)

        useCase(second)

        val persisted = repository.getPointsForSession(sessionId)
        val distance = DistanceCalculator.distanceBetween(
            LatLngPoint(persisted[0].latitude, persisted[0].longitude),
            LatLngPoint(second.latitude, second.longitude),
        )
        val expectedSpeed = (distance / 10.0).toFloat()
        assertEquals(expectedSpeed, persisted[1].speedMetersPerSec, 0.01f)
    }

    @Test
    fun givenFixWithReportedSpeed_whenRecord_thenReportedSpeedIsUsedAsIs() = runTest {
        val sessionId = repository.startSession()
        val fix = rawFix(sessionId, 10.7626, 106.6602, 1_000L, speedMetersPerSec = 3.5f)

        useCase(fix)

        val persisted = repository.getPointsForSession(sessionId)
        assertEquals(3.5f, persisted.first().speedMetersPerSec, 0.0001f)
    }

    @Test
    fun givenRejectedFixDueToBadAccuracy_whenRecord_thenNeverPassedToPersistence() = runTest {
        val sessionId = repository.startSession()
        val fix = rawFix(sessionId, 10.7626, 106.6602, 1_000L, horizontalAccuracyMeters = 50f)

        useCase(fix)

        assertTrue(repository.getPointsForSession(sessionId).isEmpty())
    }

    @Test
    fun givenJitterFix_whenRecord_thenStillPersistedAsValidSample() = runTest {
        val sessionId = repository.startSession()
        useCase(rawFix(sessionId, 10.7626, 106.6602, 1_000L, speedMetersPerSec = 0f))
        // 1m away, 1s later — classic jitter, still a valid sample per GpsFixValidator.
        val jitterFix = rawFix(sessionId, 10.762609, 106.6602, timestamp = 2_000L, speedMetersPerSec = 0f)

        useCase(jitterFix)

        assertEquals(2, repository.getPointsForSession(sessionId).size)
    }
}
