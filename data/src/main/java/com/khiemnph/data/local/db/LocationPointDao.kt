package com.khiemnph.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {

    @Insert
    suspend fun insert(point: LocationPointEntity)

    @Query("SELECT * FROM location_point WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observePointsForSession(sessionId: String): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_point WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSession(sessionId: String): List<LocationPointEntity>

    /**
     * Runs on every accepted GPS fix, so its cost is paid per fix rather than per session.
     *
     * The composite index on `(sessionId, timestamp)` is what keeps this an index seek. With only
     * `sessionId` indexed, SQLite satisfies the filter but not the `ORDER BY`, and falls back to
     * materialising and sorting every row of the session, once per fix.
     */
    @Query("SELECT * FROM location_point WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentPoint(sessionId: String): LocationPointEntity?
}
