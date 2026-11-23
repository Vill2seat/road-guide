package com.roadguide.app.data.source

/**
 * Raw weather response from Open-Meteo API.
 */
data class OpenMeteoWeather(
    val temperatureCelsius: Double?,
    val apparentTemperatureCelsius: Double?,
    val relativeHumidityPercent: Double?,
    val windSpeedMs: Double?,
    val windDirectionDegrees: Double?,
    val precipitationMm: Double?,
    val rainMm: Double?,
    val cloudCoverPercent: Double?,
    val uvIndex: Double?,
    val pm2_5: Double?,
    val pm10: Double?,
    val nitrogenDioxide: Double?,
    val ozone: Double?,
    val sulphurDioxide: Double?,
    val carbonMonoxide: Double?,
    val europeanAqi: Int?,
    val timestamp: Long
)

/**
 * Raw AQI response from CAMS API.
 */
data class CamsAqis(
    val usAqi: Int?,
    val pm2_5: Double?,
    val pm10: Double?,
    val nitrogenDioxide: Double?,
    val ozone: Double?,
    val sulphurDioxide: Double?,
    val carbonMonoxide: Double?,
    val timestamp: Long
)

/**
 * Result of environmental data fetch.
 */
data class EnvironmentalDataResult(
    val weather: OpenMeteoWeather?,
    val aqis: CamsAqis?,
    val fetchTimestamp: Long
)