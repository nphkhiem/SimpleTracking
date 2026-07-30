package com.khiemnph.data.repository

import com.khiemnph.data.local.db.LocationPointEntity
import com.khiemnph.data.local.db.SessionEntity
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.Session
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.model.SessionSummary

internal fun SessionEntity.toDomain(): Session = Session(
    id = id,
    startTimestamp = startTimestamp,
    pausedDurationMillis = pausedDurationMillis,
    status = SessionStatus.valueOf(status),
    stoppedTimestamp = stoppedTimestamp,
    finalDistanceMeters = finalDistanceMeters,
    finalAverageSpeedMps = finalAverageSpeedMps,
    routePolyline = routePolyline,
)

/** Only valid for entities with `status == STOPPED`, where the final-stat columns are populated. */
internal fun SessionEntity.toSummary(): SessionSummary {
    val stopped = requireNotNull(stoppedTimestamp) { "Stopped session $id is missing stoppedTimestamp" }
    val distance = requireNotNull(finalDistanceMeters) { "Stopped session $id is missing finalDistanceMeters" }
    val averageSpeed = requireNotNull(finalAverageSpeedMps) { "Stopped session $id is missing finalAverageSpeedMps" }
    return SessionSummary(
        id = id,
        distanceMeters = distance,
        durationMillis = stopped - startTimestamp - pausedDurationMillis,
        averageSpeedMps = averageSpeed,
        routePolyline = routePolyline,
        recordedAt = stopped,
        title = title,
    )
}

internal fun LocationPointEntity.toDomain(): LocationPoint = LocationPoint(
    sessionId = sessionId,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    horizontalAccuracyMeters = horizontalAccuracyMeters,
    speedMetersPerSec = speedMetersPerSec,
)

internal fun LocationPoint.toEntity(): LocationPointEntity = LocationPointEntity(
    sessionId = sessionId,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    horizontalAccuracyMeters = horizontalAccuracyMeters,
    speedMetersPerSec = speedMetersPerSec,
)
