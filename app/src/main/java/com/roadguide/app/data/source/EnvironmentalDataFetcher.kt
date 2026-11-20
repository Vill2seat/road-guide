package com.roadguide.app.data.source

import android.util.Log
import com.roadguide.app.data.local.EnvironmentalValidator
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.DataSource
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.domain.model.ExposureConstants

/**
 * Orchestrates fetching environmental data from multiple sources,
 * aggregates into a single EnvironmentalSample, and applies validation.
 */
class EnvironmentalDataFetcher(
    private val openMeteoSource: OpenMeteoWeatherSource,
    private val camsSource: CamsSource,
    private val validator: EnvironmentalValidator
) {

    suspend fun fetch(lat: Double, lon: Double): EnvironmentalSample? {
        Log.d("EnvFetcher", "fetching for lat=$lat, lon=$lon")
        val now = System.currentTimeMillis()
        val weather = openMeteoSource.fetch(lat, lon)
        val aqis = camsSource.fetch(lat, lon)

        val result = EnvironmentalDataResult(
            weather = weather,
            aqis = aqis,
            fetchTimestamp = now
        )

        val sample = buildSample(lat, lon, result, now)

        if (sample != null) {
            val source = when {
                weather != null && aqis != null -> DataSource.OPEN_METEO
                weather != null -> DataSource.OPEN_METEO
                aqis != null -> DataSource.CAMS
                else -> return null
            }
            Log.d("EnvFetcher", "validation result: $source")
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
        val aqis = result.aqis

        val source = when {
            weather != null && aqis != null -> DataSource.OPEN_METEO
            weather != null -> DataSource.OPEN_METEO
            aqis != null -> DataSource.CAMS
            else -> return null
        }

        val pm2_5 = weather?.pm2_5 ?: aqis?.pm2_5
        val pm10 = weather?.pm10 ?: aqis?.pm10
        val nitrogenDioxide = weather?.nitrogenDioxide ?: aqis?.nitrogenDioxide
        val ozone = weather?.ozone ?: aqis?.ozone
        val sulphurDioxide = weather?.sulphurDioxide ?: aqis?.sulphurDioxide
        val carbonMonoxide = weather?.carbonMonoxide ?: aqis?.carbonMonoxide

        val europeanAqi = weather?.europeanAqi
        val usAqi = aqis?.usAqi

        val hasWeather = weather != null
        val hasAqis = aqis != null
        val hasPm = (weather?.pm2_5 != null || aqis?.pm2_5 != null) ||
                (weather?.pm10 != null || aqis?.pm10 != null)
        val hasAqi = weather?.europeanAqi != null || aqis?.usAqi != null

        val preliminaryQuality = when {
            hasWeather && hasAqis && hasPm && hasAqi -> DataQuality.HIGH
            hasWeather && hasAqis -> DataQuality.MEDIUM
            hasWeather || hasAqis -> DataQuality.LOW
            else -> DataQuality.UNKNOWN
        }

        val exposureLevel = calculateExposureLevel(europeanAqi, usAqi, pm2_5, pm10)

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
        return calculateExposureLevel(europeanAqi, usAqi, pm2_5, pm10)
    }
}

fun calculateExposureLevel(
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
