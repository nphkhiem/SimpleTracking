package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.repository.SessionRepository

/**
 * Pauses a running session. Idempotent: a no-op if the session is not currently running
 * (already paused, already stopped, or not the active session).
 */
class PauseSessionUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String) {
        if (sessionRepository.getSessionStatus(sessionId) == SessionStatus.RUNNING) {
            sessionRepository.pauseSession(sessionId)
        }
    }
}
