package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.ActiveSessionState
import com.khiemnph.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

/** Thin wrapper over [SessionRepository.observeActiveSession]. */
class ObserveActiveSessionUseCase(private val sessionRepository: SessionRepository) {

    operator fun invoke(): Flow<ActiveSessionState?> = sessionRepository.observeActiveSession()
}
