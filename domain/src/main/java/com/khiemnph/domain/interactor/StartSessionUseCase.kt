package com.khiemnph.domain.interactor

import com.khiemnph.domain.repository.SessionRepository

/**
 * Starts a new tracking session. Idempotent: if a session is already active (running or paused),
 * returns its id instead of starting a duplicate one.
 */
class StartSessionUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(): String =
        sessionRepository.getActiveSessionId() ?: sessionRepository.startSession()
}
