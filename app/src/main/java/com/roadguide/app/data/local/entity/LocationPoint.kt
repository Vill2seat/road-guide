package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_points",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["routeId"])
    ]
)
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float? = null,
    val bearing: Float? = null,
    val altitude: Double? = null,
    val provider: String,
    val isMock: Boolean = false,
    val qualityFlags: Int = 0,
    val routeId: Long? = null,
    val collectionDiagnostics: String? = null
)
