package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stays",
    indices = [Index(value = ["routeId"]), Index(value = ["startTime"])]
data class Stay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val startTime: Long,                    // Unix ms
    val endTime: Long,
    val durationSeconds: Long = 0,
    val confidence: Double = 0.0,
    val indoorConfidence: Double = 0.0
)
