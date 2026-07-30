package com.khiemnph.domain.interactor

import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.domain.util.Split
import com.khiemnph.domain.util.SplitsCalculator

/**
 * Splits computed from the session's recorded points, not from its stored polyline.
 *
 * The polyline is downsampled to at most 64 points for drawing, which is far too coarse to time a
 * kilometre. The recorded points are the real record.
 */
class GetSessionSplitsUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String): List<Split> =
        SplitsCalculator.splitsFor(sessionRepository.getPointsForSession(sessionId))
}
