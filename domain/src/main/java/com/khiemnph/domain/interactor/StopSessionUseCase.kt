package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.domain.util.DistanceCalculator
import com.khiemnph.domain.util.RoutePolyline

/**
 * Stops a session: computes the final distance and average speed from its persisted points, then
 * delegates the stop-and-persist to [SessionRepository.stopSession].
 *
 * The route's shape is encoded here too, from the same points, so a finished session can draw
 * itself without a map. Nothing is passed in: everything this writes is derived from what was
 * recorded.
 *
 * Distance comes from [DistanceCalculator.travelledDistanceMeters], so hops that are GPS drift
 * rather than movement are excluded — a session recorded standing still totals zero, not the sum of
 * its wobble.
 *
 * Average speed is not computed here: [SessionRepository.stopSession] derives it from the distance
 * below and the session's moving time, which is the only place both are known.
 */
class StopSessionUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String): SessionSummary {
        val points = sessionRepository.getPointsForSession(sessionId)

        return sessionRepository.stopSession(
            sessionId = sessionId,
            finalDistanceMeters = DistanceCalculator.travelledDistanceMeters(points),
            routePolyline = RoutePolyline.encode(points.map { LatLngPoint(it.latitude, it.longitude) }),
        )
    }
}
