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
)
