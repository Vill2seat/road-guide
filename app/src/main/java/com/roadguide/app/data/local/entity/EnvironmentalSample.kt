package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "environmental_samples",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["latitude", "longitude"]),
        Index(value = ["source"])
    ]
)
data class EnvironmentalSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val pm2_5: Double? = null,
    val pm10: Double? = null,
    val nitrogenDioxide: Double? = null,
    val ozone: Double? = null,
    val sulphurDioxide: Double? = null,
    val carbonMonoxide: Double? = null,
    val usAqi: Int? = null,
    val europeanAqi: Int? = null,
    val temperatureCelsius: Double? = null,
    val apparentTemperatureCelsius: Double? = null,
    val relativeHumidityPercent: Double? = null,
    val windSpeedMs: Double? = null,
    val windDirectionDegrees: Double? = null,
    val precipitationMm: Double? = null,
    val rainMm: Double? = null,
    val cloudCoverPercent: Double? = null,
    val uvIndex: Double? = null,
    val source: DataSource = DataSource.OPEN_METEO,
    val dataQuality: DataQuality = DataQuality.UNKNOWN,
    val modelResolutionKm: Double? = null,
    val fetchedAt: Long = 0L
)

enum class DataSource { OPEN_METEO, OPEN_METEO_AQ, CAMS, OVERPASS, MANUAL }
enum class DataQuality { HIGH, MEDIUM, LOW, UNKNOWN, STALE, OUT_OF_RANGE }
