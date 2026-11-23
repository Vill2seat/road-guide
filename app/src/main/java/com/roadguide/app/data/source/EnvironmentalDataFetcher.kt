package com.roadguide.app.data.source

import android.util.Log
import com.roadguide.app.data.local.EnvironmentalValidator
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.DataSource
import com.roadguide.app.data.local.entity.EnvironmentalSample

/**
 * Orchestrates fetching environmental data from Open-Meteo (weather + AQI),
 * aggregates into a single EnvironmentalSample, and applies validation.
 */
class EnvironmentalDataFetcher(
    private val openMeteoSource: OpenMeteoWeatherSource,
    private val validator: EnvironmentalValidator
) {

    suspend fun fetch(lat: Double, lon: Double): EnvironmentalSample? {
        Log.d("EnvFetcher", "fetching for lat=$lat, lon=$lon")
        val now = System.currentTimeMillis()
        val weather = openMeteoSource.fetch(lat, lon)

        val result = EnvironmentalDataResult(
            weather = weather,
            aqis = null,
            fetchTimestamp = now
        )

        val sample = buildSample(lat, lon, result, now)

        if (sample != null) {
            Log.d("EnvFetcher", "validation result: ${sample.source}")
        }

        return sample
    }

    private fun buildSample(
        lat: Double,
        lon: Double,
        result: EnvironmentalDataResult,
        fetchedAt: Long
    ): EnvironmentalSample? {
        val weather = result.weather

        val source = if (weather != null) DataSource.OPEN_METEO else return null

        val pm2_5 = weather?.pm2_5
        val pm10 = weather?.pm10
        val nitrogenDioxide = weather?.nitrogenDioxide
        val ozone = weather?.ozone
        val sulphurDioxide = weather?.sulphurDioxide
        val carbonMonoxide = weather?.carbonMonoxide

        val europeanAqi = weather?.europeanAqi
        val usAqi = null

        val hasWeather = weather != null
        val hasPm = (weather?.pm2_5 != null) || (weather?.pm10 != null)
        val hasAqi = weather?.europeanAqi != null

        val preliminaryQuality = when {
            hasWeather && hasPm && hasAqi -> DataQuality.HIGH
            hasWeather -> DataQuality.MEDIUM
            else -> DataQuality.LOW
        }

        val exposureLevel = computeExposureLevel(europeanAqi, usAqi, pm2_5, pm10)

        val sample = EnvironmentalSample(
            latitude = lat, longitude = lon, timestamp = fetchedAt,
            pm2_5 = pm2_5, pm10 = pm10,
            nitrogenDioxide = nitrogenDioxide, ozone = ozone,
            sulphurDioxide = sulphurDioxide, carbonMonoxide = carbonMonoxide,
            usAqi = usAqi, europeanAqi = europeanAqi,
            temperatureCelsius = weather?.temperatureCelsius,
            apparentTemperatureCelsius = weather?.apparentTemperatureCelsius,
            relativeHumidityPercent = weather?.relativeHumidityPercent,
            windSpeedMs = weather?.windSpeedMs,
            windDirectionDegrees = weather?.windDirectionDegrees,
            precipitationMm = weather?.precipitationMm,
            rainMm = weather?.rainMm,
            cloudCoverPercent = weather?.cloudCoverPercent,
            uvIndex = weather?.uvIndex,
            source = source,
            dataQuality = preliminaryQuality,
            modelResolutionKm = null,
            fetchedAt = fetchedAt
        )

        return validator.validate(
            sample = sample,
            requestLat = lat,
            requestLon = lon,
            requestTime = fetchedAt,
            now = fetchedAt
        )?.let { validatedQuality ->
            sample.copy(dataQuality = validatedQuality)
        }
    }

    fun computeExposureLevel(
        europeanAqi: Int?,
        usAqi: Int?,
        pm2_5: Double?,
        pm10: Double?
    ): Double {
        val aqi = europeanAqi ?: usAqi ?: return 0.0
        val aqiLevel = (aqi.coerceIn(0, 500) / 500.0)
        val pmAdjustment = when {
            pm2_5 != null -> pm2_5.coerceIn(0.0, 100.0) / 100.0
            else -> 0.0
        }
        return (aqiLevel * 0.7 + pmAdjustment * 0.3).coerceIn(0.0, 1.0)
    }
}