package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exposure_results",
    indices = [Index(value = ["segmentId"]), Index(value = ["stayId"])], 
    foreignKeys = [
        ForeignKey(
            entity = RouteSegment::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExposureResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val segmentId: Long? = null,
    val stayId: Long? = null,
    val routeId: Long? = null,
    val exposureScore: Double, // 0-100 (normalized)
    val rawExposureScore: Double = 0.0, // pre-normalization raw score
    val pollution: Double,
    val durationMinutes: Int,
    val activityLevel: Double, // 0-1
    val confidence: Double = 0.0,
    val coverage: Double = 0.0, // 0-100% — процент точек с env-данными
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val MAX_EXPOSURE = 100.0
    }
}
