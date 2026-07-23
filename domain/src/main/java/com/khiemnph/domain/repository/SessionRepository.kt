package com.khiemnph.domain.repository

import com.khiemnph.domain.model.ActiveSessionState
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.SessionSummary
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    fun observeActiveSession(): Flow<ActiveSessionState?>

    suspend fun startSession(): String

    suspend fun pauseSession(sessionId: String)

    suspend fun resumeSession(sessionId: String)

    suspend fun stopSession(
        sessionId: String,
        thumbnailPath: String?,
        finalDistanceMeters: Double,
        finalAverageSpeedMps: Float,
    ): SessionSummary

    suspend fun getActiveSessionId(): String?

    fun observeSessionSummaries(): Flow<List<SessionSummary>>

    suspend fun getPointsForSession(sessionId: String): List<LocationPoint>

    suspend fun recordLocationPoint(point: LocationPoint)
}
