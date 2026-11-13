package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exposure_results",
    indices = [
        Index(value = ["routeId"]),
        Index(value = ["segmentId"]),
        Index(value = ["stayId"]),
        Index(value = ["calculationVersion"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RouteSegment::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Stay::class,
            parentColumns = ["id"],
            childColumns = ["stayId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExposureResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val segmentId: Long? = null,
    val stayId: Long? = null,
    val score: Double = 0.0,
    val normalizedScore: Double = 0.0,
    val confidence: Double = 0.0,
    val coverage: Double = 0.0,
    val pollutionFactor: Double = 0.0,
    val timeFactor: Double = 0.0,
    val activityFactor: Double = 0.0,
    val roadFactor: Double = 0.0,
    val weatherFactor: Double = 0.0,
    val skinFactor: Double = 1.0,
    val calculationVersion: Int = 1,
    val createdAt: Long = 0L
)
