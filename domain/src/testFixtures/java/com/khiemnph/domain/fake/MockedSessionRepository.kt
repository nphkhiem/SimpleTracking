package com.khiemnph.domain.fake

import com.khiemnph.domain.model.ActiveSessionState
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.Session
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.domain.util.DistanceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * In-memory fake of [SessionRepository] for tests. Not thread-safe — intended for
 * single-threaded test execution only.
 */
class MockedSessionRepository : SessionRepository {

    private val sessionsById = mutableMapOf<String, Session>()
    private val pointsBySessionId = mutableMapOf<String, MutableList<LocationPoint>>()
    private val activeSessionStateFlow = MutableStateFlow<ActiveSessionState?>(null)
    private val sessionSummariesFlow = MutableStateFlow<List<SessionSummary>>(emptyList())
    private var nextSessionId = 1
    private var pendingObserveActiveSessionError: Throwable? = null

    override fun observeActiveSession(): Flow<ActiveSessionState?> {
        val error = pendingObserveActiveSessionError
        return if (error != null) flow { throw error } else activeSessionStateFlow.asStateFlow()
    }

    override suspend fun startSession(): String {
        val id = "mocked-session-${nextSessionId++}"
        val session = Session(
            id = id,
            startTimestamp = System.currentTimeMillis(),
            pausedDurationMillis = 0L,
            status = SessionStatus.RUNNING,
            stoppedTimestamp = null,
            finalDistanceMeters = null,
            finalAverageSpeedMps = null,
            thumbnailPath = null,
        )
        sessionsById[id] = session
        pointsBySessionId[id] = mutableListOf()
        refreshActiveSessionState(id)
        return id
    }

    override suspend fun pauseSession(sessionId: String) {
        val session = sessionsById[sessionId] ?: return
        sessionsById[sessionId] = session.copy(status = SessionStatus.PAUSED)
        refreshActiveSessionState(sessionId)
    }

    override suspend fun resumeSession(sessionId: String) {
        val session = sessionsById[sessionId] ?: return
        sessionsById[sessionId] = session.copy(status = SessionStatus.RUNNING)
        refreshActiveSessionState(sessionId)
    }

    override suspend fun stopSession(
        sessionId: String,
        thumbnailPath: String?,
        finalDistanceMeters: Double,
        finalAverageSpeedMps: Float,
    ): SessionSummary {
        val session = sessionsById[sessionId] ?: error("Unknown session: $sessionId")
        val stoppedTimestamp = System.currentTimeMillis()
        val updated = session.copy(
            status = SessionStatus.STOPPED,
            stoppedTimestamp = stoppedTimestamp,
            finalDistanceMeters = finalDistanceMeters,
            finalAverageSpeedMps = finalAverageSpeedMps,
            thumbnailPath = thumbnailPath,
        )
        sessionsById[sessionId] = updated
        activeSessionStateFlow.value = null

        val summary = SessionSummary(
            id = sessionId,
            distanceMeters = finalDistanceMeters,
            durationMillis = stoppedTimestamp - session.startTimestamp - session.pausedDurationMillis,
            averageSpeedMps = finalAverageSpeedMps,
            thumbnailPath = thumbnailPath,
            recordedAt = stoppedTimestamp,
        )
        sessionSummariesFlow.value = sessionSummariesFlow.value + summary
        return summary
    }

    override suspend fun getActiveSessionId(): String? =
        sessionsById.values.firstOrNull { it.status != SessionStatus.STOPPED }?.id

    override fun observeSessionSummaries(): Flow<List<SessionSummary>> = sessionSummariesFlow.asStateFlow()

    override suspend fun getPointsForSession(sessionId: String): List<LocationPoint> =
        pointsBySessionId[sessionId].orEmpty().toList()

    override suspend fun getMostRecentPoint(sessionId: String): LocationPoint? =
        pointsBySessionId[sessionId]?.lastOrNull()

    override suspend fun recordLocationPoint(point: LocationPoint) {
        pointsBySessionId.getOrPut(point.sessionId) { mutableListOf() }.add(point)
        if (sessionsById.containsKey(point.sessionId)) {
            refreshActiveSessionState(point.sessionId)
        }
    }

    /**
     * Test-only hook: clears all in-memory state back to a pristine instance. Defensive rather
     * than strictly required today (each `@HiltAndroidTest` method gets its own freshly-built
     * Hilt component, and therefore a brand-new instance of this fake, in practice) - calling it
     * from a test's own setup costs nothing and removes any dependency on that lifecycle detail
     * staying true across Hilt/AndroidX Test versions.
     */
    fun reset() {
        sessionsById.clear()
        pointsBySessionId.clear()
        activeSessionStateFlow.value = null
        sessionSummariesFlow.value = emptyList()
        nextSessionId = 1
        pendingObserveActiveSessionError = null
    }

    /** Test-only hook: seeds a session directly, bypassing [startSession]. */
    fun seedSession(session: Session) {
        sessionsById[session.id] = session
        pointsBySessionId.getOrPut(session.id) { mutableListOf() }
        if (session.status != SessionStatus.STOPPED) {
            refreshActiveSessionState(session.id)
        }
    }

    /** Test-only hook: seeds persisted points directly, bypassing [recordLocationPoint]. */
    fun seedPoints(sessionId: String, points: List<LocationPoint>) {
        pointsBySessionId.getOrPut(sessionId) { mutableListOf() }.addAll(points)
    }

    /** Test-only hook: makes the next [observeActiveSession] collection throw [error] instead of emitting state. */
    fun throwOnObserveActiveSession(error: Throwable) {
        pendingObserveActiveSessionError = error
    }

    /**
     * Test-only hook: re-emits [sessionId]'s current active-session state with [elapsedDurationMillis]
     * bumped but no new route point, simulating the real repository's once-per-second ticker
     * re-emission that keeps duration advancing without a new GPS fix. [currentSpeedMps] optionally
     * overrides the carried-over value so a test can prove a consumer ignores it on a ticker-only
     * emission - the real repository never actually changes this value between fixes, but tests
     * need to distinguish "ignored because unchanged" from "ignored because keyed off route, not
     * this field" (see the [com.khiemnph.simpletracking.ui.record.RecordViewModel] smoothing tests).
     */
    fun emitTickerTick(sessionId: String, elapsedDurationMillis: Long, currentSpeedMps: Float? = null) {
        val current = activeSessionStateFlow.value ?: return
        if (current.session.id != sessionId) return
        activeSessionStateFlow.value = current.copy(
            elapsedDurationMillis = elapsedDurationMillis,
            currentSpeedMps = currentSpeedMps ?: current.currentSpeedMps,
        )
    }

    private fun refreshActiveSessionState(sessionId: String) {
        val session = sessionsById[sessionId] ?: return
        if (session.status == SessionStatus.STOPPED) {
            activeSessionStateFlow.value = null
            return
        }
        val points = pointsBySessionId[sessionId].orEmpty()
        val route = points.map { LatLngPoint(it.latitude, it.longitude) }
        activeSessionStateFlow.value = ActiveSessionState(
            session = session,
            distanceMeters = DistanceCalculator.totalDistanceMeters(route),
            elapsedDurationMillis = System.currentTimeMillis() - session.startTimestamp - session.pausedDurationMillis,
            currentSpeedMps = points.lastOrNull()?.speedMetersPerSec ?: 0f,
            averageSpeedMps = if (points.isEmpty()) 0f else points.map { it.speedMetersPerSec }.average().toFloat(),
            route = route,
        )
    }
}
