package com.khiemnph.domain.model

data class SessionSummary(
    val id: String,
    val distanceMeters: Double,
    val durationMillis: Long,
    val averageSpeedMps: Float,
    val routePolyline: String?,
    val recordedAt: Long,
)
