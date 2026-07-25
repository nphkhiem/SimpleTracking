package com.khiemnph.domain.util

import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.RawLocationFix
import com.khiemnph.domain.util.GpsFixValidator.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class GpsFixValidatorTest {

    private val previousPoint = LocationPoint(
        sessionId = "session-1",
        latitude = 10.7626,
        longitude = 106.6602,
        timestamp = 10_000L,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = 1.5f,
    )

    private fun rawFix(
        latitude: Double = 10.7626,
        longitude: Double = 106.6602,
        timestamp: Long = 15_000L,
        horizontalAccuracyMeters: Float = 5f,
    ) = RawLocationFix(
        sessionId = "session-1",
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        speedMetersPerSec = null,
    )

    @Test
    fun givenAccuracyWorseThan20m_whenValidate_thenFixRejected() {
        val candidate = rawFix(horizontalAccuracyMeters = 25f)

        val decision = GpsFixValidator.validate(candidate, previousPoint)

        assertEquals(Decision.REJECTED, decision)
    }

    @Test
    fun givenAccuracyMissingOrNonPositive_whenValidate_thenFixRejected() {
        val candidate = rawFix(horizontalAccuracyMeters = 0f)

        val decision = GpsFixValidator.validate(candidate, previousPoint)

        assertEquals(Decision.REJECTED, decision)
    }

    @Test
    fun givenFirstFixWithNoPreviousPoint_whenValidate_thenAccepted() {
        val candidate = rawFix()

        val decision = GpsFixValidator.validate(candidate, previousAccepted = null)

        assertEquals(Decision.ACCEPTED, decision)
    }

    @Test
    fun givenConsecutivePointsVeryCloseButLongElapsedTime_whenValidate_thenPointAcceptedAsPlausibleStop() {
        // Same coordinates as previousPoint (0m apart), but 30 seconds later — a runner standing still.
        val candidate = rawFix(
            latitude = previousPoint.latitude,
            longitude = previousPoint.longitude,
            timestamp = previousPoint.timestamp + 30_000L,
        )

        val decision = GpsFixValidator.validate(candidate, previousPoint)

        assertEquals(Decision.ACCEPTED, decision)
    }

    @Test
    fun givenConsecutivePointsVeryCloseAndShortElapsedTime_whenValidate_thenAcceptedAsJitter() {
        // 1m apart, 1 second later — implausible movement, classic GPS jitter.
        val candidate = rawFix(
            latitude = previousPoint.latitude + 0.000005,
            longitude = previousPoint.longitude,
            timestamp = previousPoint.timestamp + 1_000L,
        )

        val decision = GpsFixValidator.validate(candidate, previousPoint)

        assertEquals(Decision.ACCEPTED_JITTER, decision)
    }

    @Test
    fun givenConsecutivePointsFarApart_whenValidate_thenAccepted() {
        // ~111 m over 20 s ≈ 5.6 m/s — far apart, and a plausible pace.
        val candidate = rawFix(
            latitude = previousPoint.latitude + 0.001,
            longitude = previousPoint.longitude,
            timestamp = previousPoint.timestamp + 20_000L,
        )

        val decision = GpsFixValidator.validate(candidate, previousPoint)

        assertEquals(Decision.ACCEPTED, decision)
    }

    @Test
    fun givenImpliedSpeedAbovePlausibleMaximum_whenValidate_thenFixRejected() {
        // ~1.1 km in 1 s — a cold-start network fix or a cell-tower fallback, not movement.
        val candidate = rawFix(
            latitude = previousPoint.latitude + 0.01,
            longitude = previousPoint.longitude,
            timestamp = previousPoint.timestamp + 1_000L,
        )

        val decision = GpsFixValidator.validate(candidate, previousPoint)

        assertEquals(Decision.REJECTED, decision)
    }

    @Test
    fun givenImpliedSpeedJustBelowPlausibleMaximum_whenValidate_thenAccepted() {
        // ~22 m in 1 s ≈ 22 m/s (80 km/h) — fast, but reachable on a descent.
        val candidate = rawFix(
            latitude = previousPoint.latitude + 0.0002,
            longitude = previousPoint.longitude,
            timestamp = previousPoint.timestamp + 1_000L,
        )

        val decision = GpsFixValidator.validate(candidate, previousPoint)

        assertEquals(Decision.ACCEPTED, decision)
    }

    @Test
    fun givenNonPositiveElapsedTimeAndRealJump_whenValidate_thenFixRejected() {
        // Duplicate or out-of-order timestamp: no elapsed time to travel a real distance in.
        val candidate = rawFix(
            latitude = previousPoint.latitude + 0.001,
            longitude = previousPoint.longitude,
            timestamp = previousPoint.timestamp,
        )

        val decision = GpsFixValidator.validate(candidate, previousPoint)

        assertEquals(Decision.REJECTED, decision)
    }
}
