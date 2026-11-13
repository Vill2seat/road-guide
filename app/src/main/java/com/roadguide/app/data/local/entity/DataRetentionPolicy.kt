package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_retention_policy")
data class DataRetentionPolicy(
    @PrimaryKey val key: String = "default",
    val locationPointsDays: Int = 90,
    val routesDays: Int = 365,
    val environmentalSamplesHours: Int = 72,
    val exposureResultsDays: Int = 365,
    val dailyReportsDays: Int = -1,
    val autoDeleteEnabled: Boolean = true
)
