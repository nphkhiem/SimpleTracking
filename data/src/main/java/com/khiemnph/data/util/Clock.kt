package com.khiemnph.data.util

import android.os.SystemClock
import javax.inject.Inject

/**
 * Abstraction over "now", so [com.khiemnph.data.repository.SessionRepositoryImpl] can derive
 * elapsed/paused durations deterministically in tests instead of depending on real wall-clock time.
 *
 * This lives in `:data` rather than `:domain` because duration derivation is implemented entirely
 * here (Room is the source of truth, durations are recomputed from persisted timestamps at read
 * time) — `:domain`'s use cases never call this directly. If a future domain use case needs "now"
 * directly, this abstraction should move up to `:domain` instead of being duplicated.
 */
interface Clock {

    /**
     * Wall-clock time. Correct for "when did this happen", wrong for "how long has this taken":
     * it moves when an NTP correction lands, when the operator pushes a time on crossing a border,
     * or when the user edits the date, and it can move backwards.
     */
    fun nowMillis(): Long

    /**
     * Milliseconds since boot, which only ever moves forward at one second per second and is
     * unaffected by any clock change. This is what a stopwatch must be built on. It resets on
     * reboot, which is why a session stores both and falls back when the two disagree.
     */
    fun elapsedRealtimeMillis(): Long
}

class WallClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
