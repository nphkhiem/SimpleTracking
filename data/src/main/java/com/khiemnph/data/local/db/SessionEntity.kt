package com.khiemnph.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room representation of a tracking session.
 *
 * [status] mirrors [com.khiemnph.domain.model.SessionStatus.name] rather than using a Room
 * `TypeConverter` for a enum column — plain `String` keeps the raw DAO queries (`WHERE status !=
 * 'STOPPED'`) simple to read without extra converter registration.
 *
 * [pausedAtTimestamp] has no equivalent field on the domain `Session` model. It exists purely as
 * data-layer bookkeeping: [com.khiemnph.domain.repository.SessionRepository.pauseSession] and
 * `resumeSession` take only a `sessionId`, so this repository needs to remember *when* a pause
 * began in order to accumulate [pausedDurationMillis] on resume. It is non-null only while
 * [status] is `PAUSED`.
 */
@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startTimestamp: Long,
    val pausedDurationMillis: Long,
    val status: String,
    val pausedAtTimestamp: Long?,
    val stoppedTimestamp: Long?,
    val finalDistanceMeters: Double?,
    val finalAverageSpeedMps: Float?,
    val thumbnailPath: String?,
)
