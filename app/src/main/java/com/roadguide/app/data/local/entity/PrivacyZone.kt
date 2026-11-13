package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PrivacyZoneType { GEOFENCE, RADIUS, ROUTE_AREA }

@Entity(
    tableName = "privacy_zones",
    indices = [Index(value = ["isActive"])]
data class PrivacyZone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val zoneType: PrivacyZoneType = PrivacyZoneType.GEOFENCE,
    val isActive: Boolean = true,
    val createdAt: Long = 0
)
