package com.khiemnph.data.local.db

internal fun sessionEntity(
    id: String = "session-1",
    startTimestamp: Long = 0L,
    startElapsedRealtimeMillis: Long? = null,
    pausedDurationMillis: Long = 0L,
    status: String = "RUNNING",
    pausedAtTimestamp: Long? = null,
    pausedAtElapsedRealtimeMillis: Long? = null,
    stoppedTimestamp: Long? = null,
    finalDistanceMeters: Double? = null,
    finalAverageSpeedMps: Float? = null,
    routePolyline: String? = null,
) = SessionEntity(
    id = id,
    startTimestamp = startTimestamp,
    startElapsedRealtimeMillis = startElapsedRealtimeMillis,
    pausedDurationMillis = pausedDurationMillis,
    status = status,
    pausedAtTimestamp = pausedAtTimestamp,
    pausedAtElapsedRealtimeMillis = pausedAtElapsedRealtimeMillis,
    stoppedTimestamp = stoppedTimestamp,
    finalDistanceMeters = finalDistanceMeters,
    finalAverageSpeedMps = finalAverageSpeedMps,
    routePolyline = routePolyline,
)

internal fun locationPointEntity(
    sessionId: String,
    latitude: Double = 10.7626,
    longitude: Double = 106.6602,
    timestamp: Long = 0L,
    horizontalAccuracyMeters: Float = 5f,
    speedMetersPerSec: Float = 0f,
    /** Mirrors the wall clock unless a test is specifically about the two disagreeing. */
    elapsedRealtimeMillis: Long = timestamp,
) = LocationPointEntity(
    sessionId = sessionId,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    horizontalAccuracyMeters = horizontalAccuracyMeters,
    speedMetersPerSec = speedMetersPerSec,
    elapsedRealtimeMillis = elapsedRealtimeMillis,
)
