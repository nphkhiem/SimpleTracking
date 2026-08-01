package com.khiemnph.domain.model

/**
 * A candidate GPS fix as it arrives from the location provider, before acceptance validation and
 * speed resolution. Unlike [LocationPoint], [speedMetersPerSec] may be null here when the provider
 * did not report a speed reading — [RecordLocationFixUseCase] resolves it to a concrete value.
 */
data class RawLocationFix(
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val horizontalAccuracyMeters: Float,
    val speedMetersPerSec: Float?,
    /**
     * Milliseconds since boot, from a monotonic clock, used for every interval this app measures.
     *
     * [timestamp] is a wall clock and is what a person means by when the fix happened, but it can
     * jump backwards or forwards mid-run on an NTP correction or a manual change, which silently
     * corrupts pace and splits while the headline duration stays right. Intervals therefore come
     * from here and absolute time comes from [timestamp].
     *
     * Defaults to [timestamp] so a caller that has only a wall clock degrades to the old behaviour
     * rather than to nonsense, which is also exactly what MIGRATION_5_6 backfills into existing
     * rows: within one session those values are self-consistent, so historic intervals are
     * unchanged.
     */
    val elapsedRealtimeMillis: Long = timestamp,
)
