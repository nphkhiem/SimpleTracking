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
 * [routePolyline] holds the finished route's shape (see
 * [com.khiemnph.domain.util.RoutePolyline]) rather than a path to a rendered image. Storing the
 * geometry means a thumbnail can be drawn deterministically, offline, at any size and in either
 * theme, none of which a captured PNG of map tiles could do.
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
    /**
     * Monotonic counterpart of [startTimestamp], for measuring duration rather than recording when.
     * Null for sessions recorded before this existed, and meaningless after a reboot, so readers
     * must fall back to [startTimestamp] when it is absent or incoherent.
     */
    val startElapsedRealtimeMillis: Long?,
    val pausedDurationMillis: Long,
    val status: String,
    val pausedAtTimestamp: Long?,
    /** Monotonic counterpart of [pausedAtTimestamp], on the same terms. */
    val pausedAtElapsedRealtimeMillis: Long?,
    val stoppedTimestamp: Long?,
    val finalDistanceMeters: Double?,
    val finalAverageSpeedMps: Float?,
    val routePolyline: String?,

    /** A name the user chose. Null means never named, which the UI shows as the date instead. */
    val title: String? = null,
)
