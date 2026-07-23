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
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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
                pausedDurationMillis = 0L,
                status = SessionStatus.RUNNING.name,
                pausedAtTimestamp = null,
                stoppedTimestamp = null,
                finalDistanceMeters = null,
                finalAverageSpeedMps = null,
                thumbnailPath = null,
            ),
        )
        return id
    }

    override suspend fun pauseSession(sessionId: String) {
        val current = sessionDao.getById(sessionId) ?: return
        sessionDao.updateStatus(
            sessionId = sessionId,
            status = SessionStatus.PAUSED.name,
            pausedDurationMillis = current.pausedDurationMillis,
            pausedAtTimestamp = clock.nowMillis(),
        )
    }

    override suspend fun resumeSession(sessionId: String) {
        val current = sessionDao.getById(sessionId) ?: return
        val pausedAt = current.pausedAtTimestamp
        val additionalPausedMillis = if (pausedAt != null) clock.nowMillis() - pausedAt else 0L
        sessionDao.updateStatus(
            sessionId = sessionId,
            status = SessionStatus.RUNNING.name,
            pausedDurationMillis = current.pausedDurationMillis + additionalPausedMillis,
            pausedAtTimestamp = null,
        )
    }

    override suspend fun stopSession(
        sessionId: String,
        thumbnailPath: String?,
        finalDistanceMeters: Double,
        finalAverageSpeedMps: Float,
    ): SessionSummary {
        val current = requireNotNull(sessionDao.getById(sessionId)) { "Unknown session: $sessionId" }
        val stoppedTimestamp = clock.nowMillis()
        sessionDao.writeFinalStats(
            sessionId = sessionId,
            status = SessionStatus.STOPPED.name,
            stoppedTimestamp = stoppedTimestamp,
            finalDistanceMeters = finalDistanceMeters,
            finalAverageSpeedMps = finalAverageSpeedMps,
            thumbnailPath = thumbnailPath,
        )
        return SessionSummary(
            id = sessionId,
            distanceMeters = finalDistanceMeters,
            durationMillis = stoppedTimestamp - current.startTimestamp - current.pausedDurationMillis,
            averageSpeedMps = finalAverageSpeedMps,
            thumbnailPath = thumbnailPath,
            recordedAt = stoppedTimestamp,
        )
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
        return ActiveSessionState(
            session = entity.toDomain(),
            distanceMeters = DistanceCalculator.totalDistanceMeters(route),
            elapsedDurationMillis = clock.nowMillis() - entity.startTimestamp - entity.pausedDurationMillis,
            currentSpeedMps = points.lastOrNull()?.speedMetersPerSec ?: 0f,
            averageSpeedMps = if (points.isEmpty()) 0f else points.map { it.speedMetersPerSec }.average().toFloat(),
            route = route,
        )
    }
}
