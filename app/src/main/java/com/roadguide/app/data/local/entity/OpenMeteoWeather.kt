package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "open_meteo_weather")
data class OpenMeteoWeather(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L,
    val temperatureCelsius: Double? = null,
    val apparentTemperatureCelsius: Double? = null,
    val relativeHumidityPercent: Double? = null,
    val windSpeedMs: Double? = null,
    val windDirectionDegrees: Double? = null,
    val precipitationMm: Double? = null,
    val rainMm: Double? = null,
    val cloudCoverPercent: Double? = null,
    val uvIndex: Double? = null,
    val pm2_5: Double? = null,
    val pm10: Double? = null,
    val nitrogenDioxide: Double? = null,
    val ozone: Double? = null,
    val sulphurDioxide: Double? = null,
    val carbonMonoxide: Double? = null,
    val usAqi: Int? = null,
    val europeanAqi: Int? = null
)
