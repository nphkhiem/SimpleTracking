package com.khiemnph.domain.interactor

import com.khiemnph.domain.repository.SessionRepository

/**
 * Names a run, or clears its name.
 *
 * Blank input clears rather than storing an empty string, so a session is either named or not.
 * A run titled "" would show as an empty heading with no way to tell it from a bug.
 */
class RenameSessionUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String, title: String) {
        sessionRepository.renameSession(sessionId, title.trim().takeIf { it.isNotEmpty() })
    }
}
