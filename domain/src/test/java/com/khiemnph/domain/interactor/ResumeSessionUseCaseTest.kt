package com.khiemnph.domain.interactor

import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeSessionUseCaseTest {

    private val repository = MockedSessionRepository()
    private val useCase = ResumeSessionUseCase(repository)

    @Test
    fun givenPausedSession_whenResume_thenStatusBecomesRunning() = runTest {
        val sessionId = repository.startSession()
        repository.pauseSession(sessionId)

        useCase(sessionId)

        val active = repository.observeActiveSession().first()
        assertEquals(SessionStatus.RUNNING, active?.session?.status)
    }

    @Test
    fun givenAlreadyRunningSession_whenResumeInvokedAgain_thenNoStateChange() = runTest {
        val sessionId = repository.startSession()
        val runningState = repository.observeActiveSession().first()

        useCase(sessionId)

        val stateAfterResume = repository.observeActiveSession().first()
        assertEquals(runningState, stateAfterResume)
    }

    @Test
    fun givenNoActiveSession_whenResume_thenNoStateChange() = runTest {
        useCase("unknown-session")

        assertEquals(null, repository.observeActiveSession().first())
    }
}
