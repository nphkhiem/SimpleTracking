package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first

/**
 * Resumes a paused session. Idempotent: a no-op if the session is not currently paused
 * (already running, already stopped, or not the active session).
 */
class ResumeSessionUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String) {
        val active = sessionRepository.observeActiveSession().first()
        if (active?.session?.id == sessionId && active.session.status == SessionStatus.PAUSED) {
            sessionRepository.resumeSession(sessionId)
        }
    }
}
