package com.khiemnph.domain.interactor

import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.domain.util.GpxWriter

/**
 * Renders a finished session as a GPX document.
 *
 * Built from the recorded points rather than the stored polyline, for the same reason splits are:
 * the polyline is downsampled to at most 64 points for drawing, which would export a caricature of
 * the run. It also carries no times, and a track without them is not much use to anything that
 * reads GPX.
 */
class ExportSessionGpxUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(sessionId: String, name: String): String =
        GpxWriter.write(name, sessionRepository.getPointsForSession(sessionId))
}
