package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.repository.SessionRepository

/**
 * Resumes a paused session. Idempotent: a no-op if the session is not currently paused
 * (already running, already stopped, or not the active session).
 */
class ResumeSessionUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String) {
        if (sessionRepository.getSessionStatus(sessionId) == SessionStatus.PAUSED) {
            sessionRepository.resumeSession(sessionId)
        }
    }
}
