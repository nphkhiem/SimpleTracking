package com.khiemnph.domain.fake

import app.cash.turbine.test
import com.khiemnph.domain.model.RawLocationFix
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MockedLocationTrackingRepositoryTest {

    private val repository = MockedLocationTrackingRepository()

    @Test
    fun givenFixEmitted_whenCollectLocationUpdates_thenReceivesEmittedFix() = runTest {
        val fix = RawLocationFix(
            sessionId = "session-1",
            latitude = 10.7626,
            longitude = 106.6602,
            timestamp = 1_000L,
            horizontalAccuracyMeters = 5f,
            speedMetersPerSec = 2f,
        )

        repository.locationUpdates(sessionId = "session-1").test {
            repository.emitFix(fix)

            assertEquals(fix, awaitItem())
        }
    }

    @Test
    fun givenFixForDifferentSession_whenCollectLocationUpdates_thenFixIsFiltered() = runTest {
        val otherSessionFix = RawLocationFix(
            sessionId = "session-2",
            latitude = 10.7626,
            longitude = 106.6602,
            timestamp = 1_000L,
            horizontalAccuracyMeters = 5f,
            speedMetersPerSec = 2f,
        )
        val matchingFix = otherSessionFix.copy(sessionId = "session-1")

        repository.locationUpdates(sessionId = "session-1").test {
            repository.emitFix(otherSessionFix)
            repository.emitFix(matchingFix)

            assertEquals(matchingFix, awaitItem())
        }
    }
}
