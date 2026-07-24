package com.khiemnph.data.util

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
    fun nowMillis(): Long
}

class WallClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
