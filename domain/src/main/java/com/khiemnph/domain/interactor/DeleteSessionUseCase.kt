package com.khiemnph.domain.interactor

import com.khiemnph.domain.repository.SessionRepository

/**
 * Removes a recorded session and everything it captured.
 *
 * Until this existed there was no way to remove anything, which meant a single bad reading was
 * permanent: a session logged while standing still, or one inflated by a GPS jump, stayed in the
 * user's history for the life of the install with no recourse short of clearing app data.
 */
class DeleteSessionUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String) = sessionRepository.deleteSession(sessionId)
}
