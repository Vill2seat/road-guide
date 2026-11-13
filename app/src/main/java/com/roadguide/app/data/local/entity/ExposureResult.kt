package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exposure_results",
    indices = [Index(value = ["routeId"]), Index(value = ["segmentId"]), Index(value = ["stayId"])]
data class ExposureResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val segmentId: Long,
    val stayId: Long,
    val exposureValue: Double,
    val calibratedValue: Double,
    val timestamp: Long,                    // Unix ms
    val exposureType: String,               // "segment", "stay", "route"
    val modelVersion: Int = 1
)
