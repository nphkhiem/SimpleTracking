package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.LocationPoint
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
 * Average speed is the arithmetic mean of every recorded sample's speed, NOT
 * `totalDistance / totalDuration` — the two diverge whenever the path isn't a straight line at
 * constant speed, and only the arithmetic mean matches the app's spec.
 */
class StopSessionUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String, thumbnailPath: String?): SessionSummary {
        val points = sessionRepository.getPointsForSession(sessionId)

        val finalDistanceMeters = DistanceCalculator.travelledDistanceMeters(points)
        val finalAverageSpeedMps = averageSpeed(points)

        return sessionRepository.stopSession(
            sessionId = sessionId,
            thumbnailPath = thumbnailPath,
            finalDistanceMeters = finalDistanceMeters,
            finalAverageSpeedMps = finalAverageSpeedMps,
        )
    }

    private fun averageSpeed(points: List<LocationPoint>): Float {
        if (points.isEmpty()) return 0f
        return points.map { it.speedMetersPerSec }.average().toFloat()
    }
}
