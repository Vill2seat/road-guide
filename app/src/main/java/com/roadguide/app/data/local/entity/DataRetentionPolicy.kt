package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "data_retention_policy",
    indices = []
data class DataRetentionPolicy(
    @PrimaryKey(val id: Long = 0)
    val maxAgeDays: Int = 30,
    val maxDataPoints: Int = 10000,
    val cleanupSchedule: String = "daily",
    val lastCleanup: Long = 0
)
