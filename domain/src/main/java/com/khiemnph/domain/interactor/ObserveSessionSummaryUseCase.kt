package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

/** Thin wrapper over [SessionRepository.observeSessionSummary]. */
class ObserveSessionSummaryUseCase(private val sessionRepository: SessionRepository) {

    operator fun invoke(sessionId: String): Flow<SessionSummary?> =
        sessionRepository.observeSessionSummary(sessionId)
}
