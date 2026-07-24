package com.khiemnph.domain.model

data class ActiveSessionState(
    val session: Session,
    val distanceMeters: Double,
    val elapsedDurationMillis: Long,
    val currentSpeedMps: Float,
    val averageSpeedMps: Float,
    val route: List<LatLngPoint>,
)
