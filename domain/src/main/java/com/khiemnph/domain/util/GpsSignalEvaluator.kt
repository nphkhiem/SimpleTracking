package com.khiemnph.domain.util

import com.khiemnph.domain.model.GpsSignal
import com.khiemnph.domain.model.LocationPoint

/**
 * Judges the current GPS signal from the last fix the app actually accepted.
 *
 * Deliberately based on accepted fixes rather than raw provider callbacks: a fix rejected by
 * [GpsFixValidator] contributed nothing to the route, so as far as the recorded session is
 * concerned it did not arrive. A provider that is emitting nothing but rubbish should read as lost,
 * because that is what it means for the user's distance.
 */
object GpsSignalEvaluator {

    /**
     * Fixes are requested every 2 s, so this is many missed intervals rather than one slow one. Long
     * enough not to cry wolf when a single fix is late, short enough that a runner entering a tunnel
     * finds out before they have run through it.
     */
    const val LOST_AFTER_MILLIS = 30_000L

    /** Matches [GpsFixValidator]'s own acceptance ceiling: at the edge of usable, so say so. */
    const val WEAK_ABOVE_ACCURACY_METERS = 15f

    fun evaluate(lastAccepted: LocationPoint?, nowElapsedRealtimeMillis: Long): GpsSignal {
        if (lastAccepted == null) return GpsSignal.ACQUIRING

        // Both sides are now monotonic milliseconds since boot, so they share a basis and cannot
        // drift apart the way a GPS clock and a wall clock could. The floor stays: a fix can still
        // arrive with a marginally later reading than the `now` captured just before it. Treat that
        // as current rather than reporting a signal problem that is really a rounding artefact.
        val ageMillis = (nowElapsedRealtimeMillis - lastAccepted.elapsedRealtimeMillis).coerceAtLeast(0L)

        return when {
            ageMillis >= LOST_AFTER_MILLIS -> GpsSignal.LOST
            lastAccepted.horizontalAccuracyMeters > WEAK_ABOVE_ACCURACY_METERS -> GpsSignal.WEAK
            else -> GpsSignal.GOOD
        }
    }
}
