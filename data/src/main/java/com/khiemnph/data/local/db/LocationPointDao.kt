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

    /** Indexed `ORDER BY ... LIMIT 1` lookup — must stay O(log n), not a fetch-all-then-take-last. */
    @Query("SELECT * FROM location_point WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentPoint(sessionId: String): LocationPointEntity?
}
