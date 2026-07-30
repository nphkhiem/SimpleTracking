package com.khiemnph.domain.model

data class SessionSummary(
    val id: String,
    val distanceMeters: Double,
    val durationMillis: Long,
    val averageSpeedMps: Float,
    val routePolyline: String?,
    val recordedAt: Long,
    /** A name the user chose, or null to show the date instead. */
    val title: String? = null,
)
