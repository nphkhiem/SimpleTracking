package com.khiemnph.domain.util

import com.khiemnph.domain.model.GpsSignal
import com.khiemnph.domain.model.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GpsSignalEvaluatorTest {

    private fun point(timestamp: Long, accuracyMeters: Float) = LocationPoint(
        sessionId = "s1",
        latitude = 21.0285,
        longitude = 105.8542,
        timestamp = timestamp,
        horizontalAccuracyMeters = accuracyMeters,
        speedMetersPerSec = 2f,
    )

    @Test
    fun givenNoFixYet_whenEvaluated_thenSignalIsAcquiring() {
        assertEquals(GpsSignal.ACQUIRING, GpsSignalEvaluator.evaluate(lastAccepted = null, nowElapsedRealtimeMillis = 10_000L))
    }

    @Test
    fun givenARecentAccurateFix_whenEvaluated_thenSignalIsGood() {
        val signal = GpsSignalEvaluator.evaluate(point(timestamp = 9_000L, accuracyMeters = 5f), nowElapsedRealtimeMillis = 10_000L)

        assertEquals(GpsSignal.GOOD, signal)
    }

    @Test
    fun givenARecentButImpreciseFix_whenEvaluated_thenSignalIsWeak() {
        val signal = GpsSignalEvaluator.evaluate(point(timestamp = 9_000L, accuracyMeters = 18f), nowElapsedRealtimeMillis = 10_000L)

        assertEquals(GpsSignal.WEAK, signal)
    }

    /**
     * The case BUG-17 is about: fixes stop arriving, the timer keeps counting, and nothing on screen
     * says so. Time since the last accepted fix is the only signal available for it.
     */
    @Test
    fun givenNoFixForLongerThanTheTimeout_whenEvaluated_thenSignalIsLost() {
        val signal = GpsSignalEvaluator.evaluate(point(timestamp = 0L, accuracyMeters = 5f), nowElapsedRealtimeMillis = 31_000L)

        assertEquals(GpsSignal.LOST, signal)
    }

    @Test
    fun givenAFixJustInsideTheTimeout_whenEvaluated_thenSignalIsNotYetLost() {
        val signal = GpsSignalEvaluator.evaluate(point(timestamp = 0L, accuracyMeters = 5f), nowElapsedRealtimeMillis = 29_000L)

        assertEquals(GpsSignal.GOOD, signal)
    }

    /** A stale fix is stale regardless of how precise it was when it arrived. */
    @Test
    fun givenAnOldButVeryAccurateFix_whenEvaluated_thenLostWinsOverAccuracy() {
        val signal = GpsSignalEvaluator.evaluate(point(timestamp = 0L, accuracyMeters = 1f), nowElapsedRealtimeMillis = 60_000L)

        assertEquals(GpsSignal.LOST, signal)
    }

    /**
     * Fix timestamps come from the GPS clock while `now` comes from the system clock, so they can
     * disagree and produce a negative age. That must not read as "lost".
     */
    @Test
    fun givenAFixTimestampedInTheFuture_whenEvaluated_thenItIsTreatedAsCurrent() {
        val signal = GpsSignalEvaluator.evaluate(point(timestamp = 20_000L, accuracyMeters = 5f), nowElapsedRealtimeMillis = 10_000L)

        assertEquals(GpsSignal.GOOD, signal)
    }
}
