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
    indices = [Index("sessionId")],
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val horizontalAccuracyMeters: Float,
    val speedMetersPerSec: Float,
)
