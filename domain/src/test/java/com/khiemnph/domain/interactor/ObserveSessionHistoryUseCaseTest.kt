package com.khiemnph.domain.interactor

import app.cash.turbine.test
import com.khiemnph.domain.fake.MockedSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveSessionHistoryUseCaseTest {

    private val repository = MockedSessionRepository()
    private val useCase = ObserveSessionHistoryUseCase(repository)

    @Test
    fun givenNoStoppedSessions_whenObserve_thenEmitsEmptyList() = runTest {
        useCase().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun givenSessionStopped_whenObserve_thenEmitsSummaryFromRepository() = runTest {
        useCase().test {
            assertTrue(awaitItem().isEmpty())

            val sessionId = repository.startSession()
            repository.stopSession(sessionId, finalDistanceMeters = 100.0, routePolyline = null)

            val summaries = awaitItem()
            assertEquals(1, summaries.size)
            assertEquals(sessionId, summaries.first().id)
        }
    }
}
