package com.khiemnph.data.local.db

internal fun sessionEntity(
    id: String = "session-1",
    startTimestamp: Long = 0L,
    pausedDurationMillis: Long = 0L,
    status: String = "RUNNING",
    pausedAtTimestamp: Long? = null,
    stoppedTimestamp: Long? = null,
    finalDistanceMeters: Double? = null,
    finalAverageSpeedMps: Float? = null,
    thumbnailPath: String? = null,
) = SessionEntity(
    id = id,
    startTimestamp = startTimestamp,
    pausedDurationMillis = pausedDurationMillis,
    status = status,
    pausedAtTimestamp = pausedAtTimestamp,
    stoppedTimestamp = stoppedTimestamp,
    finalDistanceMeters = finalDistanceMeters,
    finalAverageSpeedMps = finalAverageSpeedMps,
    thumbnailPath = thumbnailPath,
)

internal fun locationPointEntity(
    sessionId: String,
    latitude: Double = 10.7626,
    longitude: Double = 106.6602,
    timestamp: Long = 0L,
    horizontalAccuracyMeters: Float = 5f,
    speedMetersPerSec: Float = 0f,
) = LocationPointEntity(
    sessionId = sessionId,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    horizontalAccuracyMeters = horizontalAccuracyMeters,
    speedMetersPerSec = speedMetersPerSec,
)
