package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_reports",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyReport(
    @PrimaryKey val date: String,
    val totalScore: Double = 0.0,
    val confidence: Double = 0.0,
    val coverage: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val totalDurationSeconds: Long = 0,
    val walkingDurationSeconds: Long = 0,
    val transportDurationSeconds: Long = 0,
    val stationaryDurationSeconds: Long = 0,
    val visitedPlacesCount: Int = 0,
    val topContributorsJson: String = "[]",
    val bestPeriodJson: String? = null,
    val worstPeriodJson: String? = null,
    val recommendationsJson: String = "[]",
    val reportVersion: Int = 1,
    val generatedAt: Long = 0L,
    val isRegenerated: Boolean = false
)
