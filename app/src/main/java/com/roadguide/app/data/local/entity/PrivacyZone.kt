package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "privacy_zones",
    indices = [Index(value = ["name"])]
)
data class PrivacyZone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusMeters: Double,
    val isActive: Boolean = true,
    val excludeFromHistory: Boolean = true,
    val excludeFromExposure: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
