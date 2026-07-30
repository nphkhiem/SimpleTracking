package com.khiemnph.domain.repository

import com.khiemnph.domain.model.ActiveSessionState
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.model.SessionSummary
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    fun observeActiveSession(): Flow<ActiveSessionState?>

    suspend fun startSession(): String

    suspend fun pauseSession(sessionId: String)

    suspend fun resumeSession(sessionId: String)

    /**
     * Average speed is deliberately not a parameter: it is derived here from [finalDistanceMeters]
     * and the session's own moving time, so the three numbers shown together on a history row can
     * never contradict each other.
     */
    suspend fun stopSession(
        sessionId: String,
        finalDistanceMeters: Double,
        routePolyline: String?,
    ): SessionSummary

    /**
     * Reads just the status. Exists so a caller that only needs to know whether a session is
     * running does not have to observe the whole active-session state, which loads every recorded
     * point and re-runs the distance calculation to answer a question about one enum.
     */
    suspend fun getSessionStatus(sessionId: String): SessionStatus?

    /**
     * The finished session's stats, re-emitted whenever the row changes. Null while the id is
     * unknown, while the session has not stopped yet, and again once it is deleted.
     *
     * A Flow rather than a one-shot read because both screens that need this can open while the
     * row is still moving. The post-run screen is reached the instant Stop is dispatched, before
     * the Service has written the final stats, and a session open on screen can be deleted from
     * elsewhere. A single read would show the first case as "missing" and never notice the second.
     */
    fun observeSessionSummary(sessionId: String): Flow<SessionSummary?>

    /**
     * Names a session, or clears its name when [title] is null. Blank input is the caller's to
     * reject; this stores what it is given.
     */
    suspend fun renameSession(sessionId: String, title: String?)

    /** Removes a session and everything it recorded. Unknown ids are a no-op, not an error. */
    suspend fun deleteSession(sessionId: String)

    suspend fun getActiveSessionId(): String?

    fun observeSessionSummaries(): Flow<List<SessionSummary>>

    suspend fun getPointsForSession(sessionId: String): List<LocationPoint>

    suspend fun getMostRecentPoint(sessionId: String): LocationPoint?

    suspend fun recordLocationPoint(point: LocationPoint)
}
