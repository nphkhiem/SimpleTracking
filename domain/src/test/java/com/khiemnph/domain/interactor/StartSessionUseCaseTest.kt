package com.khiemnph.domain.interactor

import com.khiemnph.domain.fake.MockedSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StartSessionUseCaseTest {

    private val repository = MockedSessionRepository()
    private val useCase = StartSessionUseCase(repository)

    @Test
    fun givenNoActiveSession_whenStart_thenNewSessionIdIsCreated() = runTest {
        val sessionId = useCase()

        assertNotNull(sessionId)
        assertEquals(sessionId, repository.getActiveSessionId())
    }

    @Test
    fun givenActiveSessionAlreadyRunning_whenStartInvokedAgain_thenExistingSessionIdIsReturned() = runTest {
        val firstId = useCase()

        val secondId = useCase()

        assertEquals(firstId, secondId)
    }
}
