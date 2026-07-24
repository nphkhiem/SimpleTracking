package com.khiemnph.domain.fake

import com.khiemnph.domain.model.LocationPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MockedSessionRepositoryTest {

    private val repository = MockedSessionRepository()

    private fun point(sessionId: String, timestamp: Long) = LocationPoint(
        sessionId = sessionId,
        latitude = 10.7626,
        longitude = 106.6602,
        timestamp = timestamp,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = 1f,
    )

    @Test
    fun givenNoPointsRecorded_whenGetMostRecentPoint_thenReturnsNull() = runTest {
        val sessionId = repository.startSession()

        assertNull(repository.getMostRecentPoint(sessionId))
    }

    @Test
    fun givenUnknownSessionId_whenGetMostRecentPoint_thenReturnsNull() = runTest {
        assertNull(repository.getMostRecentPoint("unknown-session"))
    }

    @Test
    fun givenMultiplePointsRecorded_whenGetMostRecentPoint_thenReturnsLastRecordedPoint() = runTest {
        val sessionId = repository.startSession()
        repository.recordLocationPoint(point(sessionId, timestamp = 1_000L))
        val latest = point(sessionId, timestamp = 2_000L)
        repository.recordLocationPoint(latest)

        assertEquals(latest, repository.getMostRecentPoint(sessionId))
    }
}
