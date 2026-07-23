package com.khiemnph.domain.model

data class Session(
    val id: String,
    val startTimestamp: Long,
    val pausedDurationMillis: Long,
    val status: SessionStatus,
    val stoppedTimestamp: Long?,
    val finalDistanceMeters: Double?,
    val finalAverageSpeedMps: Float?,
    val thumbnailPath: String?,
)
