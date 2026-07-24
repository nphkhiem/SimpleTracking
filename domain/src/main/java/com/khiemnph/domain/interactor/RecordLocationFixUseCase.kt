package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.RawLocationFix
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.domain.util.DistanceCalculator
import com.khiemnph.domain.util.GpsFixValidator

/**
 * Validates a raw candidate GPS fix and, if accepted, resolves its final speed before delegating
 * it onward for persistence. Rejected fixes (per [GpsFixValidator]) are discarded entirely; fixes
 * flagged as GPS jitter are still persisted as valid samples.
 */
class RecordLocationFixUseCase(private val sessionRepository: SessionRepository) {

    suspend operator fun invoke(fix: RawLocationFix) {
        val previousAccepted = sessionRepository.getMostRecentPoint(fix.sessionId)

        val decision = GpsFixValidator.validate(fix, previousAccepted)
        if (decision == GpsFixValidator.Decision.REJECTED) return

        val resolvedSpeedMetersPerSec = fix.speedMetersPerSec ?: fallbackSpeed(fix, previousAccepted)

        sessionRepository.recordLocationPoint(
            LocationPoint(
                sessionId = fix.sessionId,
                latitude = fix.latitude,
                longitude = fix.longitude,
                timestamp = fix.timestamp,
                horizontalAccuracyMeters = fix.horizontalAccuracyMeters,
                speedMetersPerSec = resolvedSpeedMetersPerSec,
            ),
        )
    }

    private fun fallbackSpeed(fix: RawLocationFix, previousAccepted: LocationPoint?): Float {
        if (previousAccepted == null) return 0f

        val elapsedSeconds = (fix.timestamp - previousAccepted.timestamp) / 1_000.0
        if (elapsedSeconds <= 0.0) return 0f

        val distanceMeters = DistanceCalculator.distanceBetween(
            LatLngPoint(previousAccepted.latitude, previousAccepted.longitude),
            LatLngPoint(fix.latitude, fix.longitude),
        )
        return (distanceMeters / elapsedSeconds).toFloat()
    }
}
