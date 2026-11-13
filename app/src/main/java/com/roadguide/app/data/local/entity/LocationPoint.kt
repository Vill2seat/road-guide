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
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,                    // Unix milliseconds (UTC)
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float?,
    val bearing: Float?,                    // Degrees 0-360
    val altitude: Double?,                  // Meters above WGS84 ellipsoid
    val provider: String,                   // "gps", "network", "fused", "passive"
    val isMock: Boolean = false,
    val qualityFlags: Int = 0,              // Bitmask: 1=low_accuracy, 2=mock, 4=jump_detected, 8=stale
    val routeId: Long? = null,              // FK to Route (nullable until segmented)
    val collectionDiagnostics: String? = null // JSON: battery, interval, source
)
