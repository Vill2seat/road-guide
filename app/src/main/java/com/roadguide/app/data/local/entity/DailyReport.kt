package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_reports",
    indices = [Index(value = ["date"])]
data class DailyReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                       // YYYY-MM-DD format
    val totalDurationSeconds: Long = 0,
    val exposureValue: Double = 0.0,
    val averageAQI: Int = 0,
    val maxAQI: Int = 0,
    val status: String = "COMPLETED"
)
