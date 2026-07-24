package com.khiemnph.domain.interactor

import app.cash.turbine.test
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.SessionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveActiveSessionUseCaseTest {

    private val repository = MockedSessionRepository()
    private val useCase = ObserveActiveSessionUseCase(repository)

    @Test
    fun givenNoActiveSession_whenObserve_thenEmitsNull() = runTest {
        useCase().test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun givenSessionStarted_whenObserve_thenEmitsRunningSessionFromRepository() = runTest {
        useCase().test {
            assertNull(awaitItem())

            val sessionId = repository.startSession()

            val active = awaitItem()
            assertEquals(sessionId, active?.session?.id)
            assertEquals(SessionStatus.RUNNING, active?.session?.status)
        }
    }
}
