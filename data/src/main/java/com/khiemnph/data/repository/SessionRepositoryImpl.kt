package com.khiemnph.data.repository

import com.khiemnph.data.local.db.LocationPointDao
import com.khiemnph.data.local.db.LocationPointEntity
import com.khiemnph.data.local.db.SessionDao
import com.khiemnph.data.local.db.SessionEntity
import com.khiemnph.data.util.Clock
import com.khiemnph.data.util.tickerFlow
import com.khiemnph.domain.model.ActiveSessionState
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.domain.util.DistanceCalculator
import com.khiemnph.domain.util.GpsSignalEvaluator
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Room is the single source of truth: elapsed duration and distance are always derived from
 * persisted timestamps/points at read time, never cached as an in-memory running counter. A
 * process restart needs no special recovery path — recomputing from Room on the next
 * [observeActiveSession] collection IS the normal path.
 */
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val locationPointDao: LocationPointDao,
    private val clock: Clock,
) : SessionRepository {

    /**
     * Serialises the read-modify-write in pause/resume/stop. The DAO's status guards are what make
     * a stale write inert, but they cannot stop two in-flight commands from both reading the same
     * `pausedDurationMillis` and one losing its update; this orders them within the process.
     */
    private val sessionWriteMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveSession(): Flow<ActiveSessionState?> =
        sessionDao.observeActiveSession()
            .flatMapLatest { entity ->
                if (entity == null) {
                    flowOf(null)
                } else {
                    combine(
                        locationPointDao.observePointsForSession(entity.id),
                        tickerFlow(1.seconds),
                    ) { points, _ -> computeActiveSessionState(entity, points) }
                }
            }

    override suspend fun startSession(): String {
        val id = UUID.randomUUID().toString()
        sessionDao.upsert(
            SessionEntity(
                id = id,
                startTimestamp = clock.nowMillis(),
                startElapsedRealtimeMillis = clock.elapsedRealtimeMillis(),
                pausedDurationMillis = 0L,
                status = SessionStatus.RUNNING.name,
                pausedAtTimestamp = null,
                pausedAtElapsedRealtimeMillis = null,
                stoppedTimestamp = null,
                finalDistanceMeters = null,
                finalAverageSpeedMps = null,
                routePolyline = null,
            ),
        )
        return id
    }

    override suspend fun pauseSession(sessionId: String) = sessionWriteMutex.withLock {
        val current = sessionDao.getById(sessionId) ?: return@withLock
        sessionDao.updateStatusIfCurrent(
            sessionId = sessionId,
            expectedCurrentStatus = SessionStatus.RUNNING.name,
            status = SessionStatus.PAUSED.name,
            pausedDurationMillis = current.pausedDurationMillis,
            pausedAtTimestamp = clock.nowMillis(),
            pausedAtElapsedRealtimeMillis = clock.elapsedRealtimeMillis(),
        )
        Unit
    }

    override suspend fun resumeSession(sessionId: String) = sessionWriteMutex.withLock {
        val current = sessionDao.getById(sessionId) ?: return@withLock
        val additionalPausedMillis = pausedIntervalMillis(current, clock.nowMillis(), clock.elapsedRealtimeMillis())
        sessionDao.updateStatusIfCurrent(
            sessionId = sessionId,
            expectedCurrentStatus = SessionStatus.PAUSED.name,
            status = SessionStatus.RUNNING.name,
            pausedDurationMillis = current.pausedDurationMillis + additionalPausedMillis,
            pausedAtTimestamp = null,
            pausedAtElapsedRealtimeMillis = null,
        )
        Unit
    }

    /**
     * Idempotent: stopping an already-stopped session returns what was persisted rather than
     * recomputing it, so a redelivered stop cannot extend a finished session's duration.
     */
    override suspend fun stopSession(
        sessionId: String,
        finalDistanceMeters: Double,
        routePolyline: String?,
    ): SessionSummary = sessionWriteMutex.withLock {
        val current = requireNotNull(sessionDao.getById(sessionId)) { "Unknown session: $sessionId" }
        if (current.status == SessionStatus.STOPPED.name) return@withLock current.toSummary()

        val stoppedTimestamp = clock.nowMillis()
        val durationMillis = elapsedMillis(current, stoppedTimestamp)
        val finalAverageSpeedMps = averageSpeedMps(finalDistanceMeters, durationMillis)
        sessionDao.writeFinalStats(
            sessionId = sessionId,
            status = SessionStatus.STOPPED.name,
            stoppedTimestamp = stoppedTimestamp,
            pausedDurationMillis = finalPausedDurationMillis(current, stoppedTimestamp),
            finalDistanceMeters = finalDistanceMeters,
            finalAverageSpeedMps = finalAverageSpeedMps,
            routePolyline = routePolyline,
        )
        SessionSummary(
            id = sessionId,
            distanceMeters = finalDistanceMeters,
            durationMillis = durationMillis,
            averageSpeedMps = finalAverageSpeedMps,
            routePolyline = routePolyline,
            recordedAt = stoppedTimestamp,
        )
    }

    override suspend fun getSessionStatus(sessionId: String): SessionStatus? =
        sessionDao.getStatusById(sessionId)?.let(SessionStatus::valueOf)

    override fun observeSessionSummary(sessionId: String): Flow<SessionSummary?> =
        sessionDao.observeById(sessionId).map { entity ->
            entity?.takeIf { it.status == SessionStatus.STOPPED.name }?.toSummary()
        }

    override suspend fun renameSession(sessionId: String, title: String?) {
        sessionDao.updateTitle(sessionId, title)
    }

    override suspend fun deleteSession(sessionId: String) = sessionWriteMutex.withLock {
        sessionDao.deleteById(sessionId)
    }

    override suspend fun getActiveSessionId(): String? = sessionDao.getActiveSessionId()

    override fun observeSessionSummaries(): Flow<List<SessionSummary>> =
        sessionDao.observeSummaries().map { entities -> entities.map { it.toSummary() } }

    override suspend fun getPointsForSession(sessionId: String): List<LocationPoint> =
        locationPointDao.getPointsForSession(sessionId).map { it.toDomain() }

    override suspend fun getMostRecentPoint(sessionId: String): LocationPoint? =
        locationPointDao.getMostRecentPoint(sessionId)?.toDomain()

    override suspend fun recordLocationPoint(point: LocationPoint) {
        locationPointDao.insert(point.toEntity())
    }

    private fun computeActiveSessionState(
        entity: SessionEntity,
        points: List<LocationPointEntity>,
    ): ActiveSessionState {
        val route = points.map { LatLngPoint(it.latitude, it.longitude) }
        // Drift-aware: every recorded point stays in `route` for the trace, but hops that are GPS
        // wobble rather than movement must not inflate the distance readout.
        val domainPoints = points.map { it.toDomain() }
        val distanceMeters = DistanceCalculator.travelledDistanceMeters(domainPoints)
        val now = clock.nowMillis()
        val elapsedDurationMillis = elapsedMillis(entity, now)
        return ActiveSessionState(
            session = entity.toDomain(),
            distanceMeters = distanceMeters,
            elapsedDurationMillis = elapsedDurationMillis,
            currentSpeedMps = points.lastOrNull()?.speedMetersPerSec ?: 0f,
            averageSpeedMps = averageSpeedMps(distanceMeters, elapsedDurationMillis),
            route = route,
            // Recomputed on every ticker emission, which is what makes a signal that has gone
            // quiet visible: nothing new has to arrive for the state to change.
            gpsSignal = GpsSignalEvaluator.evaluate(domainPoints.lastOrNull(), clock.elapsedRealtimeMillis()),
        )
    }

    /**
     * Distance over moving time, which is what the distance and duration on screen actually imply.
     * A mean of the provider's per-fix speed samples would be a different quantity: sampling-rate
     * weighted, averaging in every stopped-at-a-light zero, and unreconcilable with the other two
     * numbers on the same row.
     *
     * Guards a non-positive duration, which is reachable on a session stopped the same millisecond
     * it started, and would otherwise yield infinity or NaN.
     */
    private fun averageSpeedMps(distanceMeters: Double, durationMillis: Long): Float =
        if (durationMillis <= 0L) 0f else (distanceMeters / (durationMillis / 1_000.0)).toFloat()

    /**
     * How long an interval has really lasted, preferring the clock that cannot jump.
     *
     * [monotonicStart] is used when it is present and coherent. It is absent for sessions recorded
     * before the monotonic columns existed, and it is incoherent after a reboot, because
     * `elapsedRealtime` restarts from zero and so reads as earlier than a start captured before the
     * reboot. In both cases wall-clock time is the only measurement available and is used instead.
     *
     * The result is floored at zero either way. A backwards clock change would otherwise produce a
     * negative duration, which the timer used to render as text like `-0:-15`.
     */
    private fun intervalMillis(
        wallStart: Long,
        wallNow: Long,
        monotonicStart: Long?,
        monotonicNow: Long,
    ): Long {
        val monotonic = monotonicStart?.takeIf { monotonicNow >= it }?.let { monotonicNow - it }
        return (monotonic ?: (wallNow - wallStart)).coerceAtLeast(0L)
    }

    /** The in-progress paused interval, or zero when the session is not paused. */
    private fun pausedIntervalMillis(entity: SessionEntity, wallNow: Long, monotonicNow: Long): Long {
        val pausedAt = entity.pausedAtTimestamp ?: return 0L
        return intervalMillis(pausedAt, wallNow, entity.pausedAtElapsedRealtimeMillis, monotonicNow)
    }

    /**
     * Elapsed session duration as of [now]: wall-clock time since start, minus all completed
     * paused intervals ([SessionEntity.pausedDurationMillis]), minus the in-progress paused
     * interval if the session is currently [SessionStatus.PAUSED] (from [SessionEntity.pausedAtTimestamp]
     * up to [now]). Without this last term, a session sitting paused would keep accruing elapsed
     * time as if it were still running.
     */
    private fun elapsedMillis(entity: SessionEntity, now: Long): Long {
        val sinceStart = intervalMillis(
            wallStart = entity.startTimestamp,
            wallNow = now,
            monotonicStart = entity.startElapsedRealtimeMillis,
            monotonicNow = clock.elapsedRealtimeMillis(),
        )
        return (sinceStart - finalPausedDurationMillis(entity, now)).coerceAtLeast(0L)
    }

    /**
     * Total paused time across the whole session as of [now]: all completed paused intervals
     * ([SessionEntity.pausedDurationMillis]) plus the in-progress paused interval if the session
     * is currently [SessionStatus.PAUSED] (from [SessionEntity.pausedAtTimestamp] up to [now]).
     * When [now] is the stop timestamp, this is also the value to persist so a later read of the
     * stopped row (e.g. via `toSummary()`) doesn't need its own paused-state branch.
     */
    private fun finalPausedDurationMillis(entity: SessionEntity, now: Long): Long {
        val ongoingPauseMillis = if (entity.status == SessionStatus.PAUSED.name) {
            pausedIntervalMillis(entity, now, clock.elapsedRealtimeMillis())
        } else {
            0L
        }
        return entity.pausedDurationMillis + ongoingPauseMillis
    }
}
