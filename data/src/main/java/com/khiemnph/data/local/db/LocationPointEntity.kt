package com.khiemnph.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room representation of a single resolved GPS sample recorded during a session. */
@Entity(
    tableName = "location_point",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // Composite, and in this order, because the hot query filters on sessionId and orders by
    // timestamp. A sessionId-only index serves the filter but not the ordering, which leaves
    // SQLite sorting every row of the session on every GPS fix.
    indices = [Index(value = ["sessionId", "timestamp"])],
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val horizontalAccuracyMeters: Float,
    val speedMetersPerSec: Float,
    /** Monotonic milliseconds since boot. Every interval this app measures comes from here. */
    val elapsedRealtimeMillis: Long,
)
