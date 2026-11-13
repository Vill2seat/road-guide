package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    indices = [Index(value = ["startTime"]), Index(value = ["date"])],
    foreignKeys = [ForeignKey(
        entity = RouteSegment::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )]
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,                    // Unix ms (local timezone)
    val endTime: Long?,                     // Null if still active
    val distanceMeters: Double = 0.0,
    val status: RouteStatus = RouteStatus.IN_PROGRESS,
    val routeType: RouteType = RouteType.DAILY_AUTO,
    val confidence: Double = 0.0,           // 0.0–1.0 overall route confidence
    val date: String,                       // YYYY-MM-DD (local date of startTime)
    val totalDurationSeconds: Long = 0,
    val calculationVersion: Int = 1         // Exposure model version used
)
enum class RouteStatus { IN_PROGRESS, COMPLETED, PAUSED, CORRUPTED }
enum class RouteType { DAILY_AUTO, MANUAL_TRACKING, PLANNED_ROUTE }
