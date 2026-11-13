package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "environmental_samples",
    indices = [
        Index(value = ["latitude", "longitude"]),
        Index(value = ["timestamp"])
    ]
data class EnvironmentalSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,                    // Unix ms
    val aqi: Int,                           // Air Quality Index
    val noiseLevel: Float,
    val temperature: Double,
    val humidity: Float,
    val weatherCode: Int,
    val dataQuality: String = "OK",
    val fetchedAt: Long = 0
)
