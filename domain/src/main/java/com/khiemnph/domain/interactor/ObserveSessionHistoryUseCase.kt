package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

/** Thin wrapper over [SessionRepository.observeSessionSummaries]. */
class ObserveSessionHistoryUseCase(private val sessionRepository: SessionRepository) {

    operator fun invoke(): Flow<List<SessionSummary>> = sessionRepository.observeSessionSummaries()
}
