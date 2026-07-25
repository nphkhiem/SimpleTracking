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

    /**
     * Status-guarded so a write computed against a stale read cannot land. Two commands can be in
     * flight at once (in-app buttons and the notification's own actions both dispatch into the same
     * use cases), and without the guard a Pause that read `RUNNING` before a Stop committed would
     * resurrect the finished session as `PAUSED`, carrying its final stats.
     *
     * @return rows updated: 0 means the session had already moved on and this write was correctly
     * discarded.
     */
    @Query(
        """
        UPDATE session
        SET status = :status, pausedDurationMillis = :pausedDurationMillis, pausedAtTimestamp = :pausedAtTimestamp
        WHERE id = :sessionId AND status = :expectedCurrentStatus
        """,
    )
    suspend fun updateStatusIfCurrent(
        sessionId: String,
        expectedCurrentStatus: String,
        status: String,
        pausedDurationMillis: Long,
        pausedAtTimestamp: Long?,
    ): Int

    /**
     * Never rewrites an already-stopped session, which also makes a redelivered `ACTION_STOP` inert
     * instead of silently extending the finished session's duration to the moment of redelivery.
     *
     * @return rows updated: 0 means it was already stopped.
     */
    @Query(
        """
        UPDATE session
        SET status = :status, stoppedTimestamp = :stoppedTimestamp, pausedDurationMillis = :pausedDurationMillis,
            pausedAtTimestamp = NULL, finalDistanceMeters = :finalDistanceMeters,
            finalAverageSpeedMps = :finalAverageSpeedMps, thumbnailPath = :thumbnailPath
        WHERE id = :sessionId AND status != 'STOPPED'
        """,
    )
    suspend fun writeFinalStats(
        sessionId: String,
        status: String,
        stoppedTimestamp: Long,
        pausedDurationMillis: Long,
        finalDistanceMeters: Double,
        finalAverageSpeedMps: Float,
        thumbnailPath: String?,
    ): Int

    // ORDER BY makes recovery deterministic. Nothing enforces a single active row at the schema
    // level, so if one ever slips through, "whichever row SQLite happened to return" must not be
    // the rule that decides which session the user resumes.
    @Query("SELECT * FROM session WHERE status != 'STOPPED' ORDER BY startTimestamp DESC LIMIT 1")
    fun observeActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM session WHERE status = 'STOPPED' ORDER BY stoppedTimestamp DESC")
    fun observeSummaries(): Flow<List<SessionEntity>>

    @Query("SELECT id FROM session WHERE status != 'STOPPED' ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getActiveSessionId(): String?
}
