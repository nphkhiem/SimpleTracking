package com.khiemnph.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM session WHERE id = :sessionId")
    suspend fun getById(sessionId: String): SessionEntity?

    @Query(
        """
        UPDATE session
        SET status = :status, pausedDurationMillis = :pausedDurationMillis, pausedAtTimestamp = :pausedAtTimestamp
        WHERE id = :sessionId
        """,
    )
    suspend fun updateStatus(
        sessionId: String,
        status: String,
        pausedDurationMillis: Long,
        pausedAtTimestamp: Long?,
    )

    @Query(
        """
        UPDATE session
        SET status = :status, stoppedTimestamp = :stoppedTimestamp, finalDistanceMeters = :finalDistanceMeters,
            finalAverageSpeedMps = :finalAverageSpeedMps, thumbnailPath = :thumbnailPath
        WHERE id = :sessionId
        """,
    )
    suspend fun writeFinalStats(
        sessionId: String,
        status: String,
        stoppedTimestamp: Long,
        finalDistanceMeters: Double,
        finalAverageSpeedMps: Float,
        thumbnailPath: String?,
    )

    @Query("SELECT * FROM session WHERE status != 'STOPPED' LIMIT 1")
    fun observeActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM session WHERE status = 'STOPPED' ORDER BY stoppedTimestamp DESC")
    fun observeSummaries(): Flow<List<SessionEntity>>

    @Query("SELECT id FROM session WHERE status != 'STOPPED' LIMIT 1")
    suspend fun getActiveSessionId(): String?
}
