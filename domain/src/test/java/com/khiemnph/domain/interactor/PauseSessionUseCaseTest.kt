package com.khiemnph.domain.interactor

import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PauseSessionUseCaseTest {

    private val repository = MockedSessionRepository()
    private val useCase = PauseSessionUseCase(repository)

    @Test
    fun givenRunningSession_whenPause_thenStatusBecomesPaused() = runTest {
        val sessionId = repository.startSession()

        useCase(sessionId)

        val active = repository.observeActiveSession().first()
        assertEquals(SessionStatus.PAUSED, active?.session?.status)
    }

    @Test
    fun givenAlreadyPausedSession_whenPauseInvokedAgain_thenNoStateChange() = runTest {
        val sessionId = repository.startSession()
        useCase(sessionId)
        val pausedState = repository.observeActiveSession().first()

        useCase(sessionId)

        val stateAfterSecondPause = repository.observeActiveSession().first()
        assertEquals(pausedState, stateAfterSecondPause)
    }

    @Test
    fun givenNoActiveSession_whenPause_thenNoStateChange() = runTest {
        useCase("unknown-session")

        assertEquals(null, repository.observeActiveSession().first())
    }
}
