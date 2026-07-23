package com.khiemnph.domain.fake

import app.cash.turbine.test
import com.khiemnph.domain.model.LocationPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MockedLocationTrackingRepositoryTest {

    private val repository = MockedLocationTrackingRepository()

    @Test
    fun givenFixEmitted_whenCollectLocationUpdates_thenReceivesEmittedFix() = runTest {
        val point = LocationPoint(
            sessionId = "session-1",
            latitude = 10.7626,
            longitude = 106.6602,
            timestamp = 1_000L,
            horizontalAccuracyMeters = 5f,
            speedMetersPerSec = 2f,
        )

        repository.locationUpdates().test {
            repository.emitFix(point)

            assertEquals(point, awaitItem())
        }
    }
}
