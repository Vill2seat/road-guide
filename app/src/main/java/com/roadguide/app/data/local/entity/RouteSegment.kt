package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_segments",
    indices = [
        Index(value = ["routeId"]),
        Index(value = ["startTime"]),
        Index(value = ["activityType"]),
    ],
    foreignKeys = [ForeignKey(
        entity = Route::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )]
data class RouteSegment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val startTime: Long,                    // Unix ms
    val endTime: Long,
    val activityType: Int,                  // Activity type code
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0
)
