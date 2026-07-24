package com.khiemnph.domain.model

data class LocationPoint(
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val horizontalAccuracyMeters: Float,
    val speedMetersPerSec: Float,
)
