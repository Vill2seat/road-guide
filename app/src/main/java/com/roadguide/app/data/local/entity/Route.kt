package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    indices = [Index(value = ["startTime"]), Index(value = ["date"])]
)
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Double = 0.0,
    val status: RouteStatus = RouteStatus.IN_PROGRESS,
    val routeType: RouteType = RouteType.DAILY_AUTO,
    val confidence: Double = 0.0,
    val date: String,
    val totalDurationSeconds: Long = 0,
    val calculationVersion: Int = 1
)

enum class RouteStatus { IN_PROGRESS, COMPLETED, PAUSED, CORRUPTED }
enum class RouteType { DAILY_AUTO, MANUAL_TRACKING, PLANNED_ROUTE }
