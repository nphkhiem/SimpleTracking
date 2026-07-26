package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.domain.util.DistanceCalculator

/**
 * Stops a session: computes the final distance and average speed from its persisted points, then
 * delegates the stop-and-persist to [SessionRepository.stopSession].
 *
 * Distance comes from [DistanceCalculator.travelledDistanceMeters], so hops that are GPS drift
 * rather than movement are excluded — a session recorded standing still totals zero, not the sum of
 * its wobble.
 *
 * Average speed is not computed here: [SessionRepository.stopSession] derives it from the distance
 * below and the session's moving time, which is the only place both are known.
 */
class StopSessionUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String, thumbnailPath: String?): SessionSummary {
        val points = sessionRepository.getPointsForSession(sessionId)

        return sessionRepository.stopSession(
            sessionId = sessionId,
            thumbnailPath = thumbnailPath,
            finalDistanceMeters = DistanceCalculator.travelledDistanceMeters(points),
        )
    }
}
