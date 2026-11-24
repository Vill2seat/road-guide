package com.roadguide.app.data.source

import android.util.Log
import com.roadguide.app.data.local.EnvironmentalValidator
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.DataSource
import com.roadguide.app.data.local.entity.EnvironmentalSample
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class EnvironmentalDataFetcher(
    private val openMeteoSource: OpenMeteoWeatherSource,
    private val airQualitySource: OpenMeteoAirQualitySource,
    private val validator: EnvironmentalValidator
) {

    suspend fun fetch(
        latitude: Double,
        longitude: Double,
        requestedTime: Long = System.currentTimeMillis()
    ): EnvironmentalSample? {
        Log.d("EnvFetcher", "fetching weather and air quality for coordinates")
        val fetchedAt = System.currentTimeMillis()
        val (weather, airQuality) = coroutineScope {
            val weatherRequest = async { openMeteoSource.fetch(latitude, longitude) }
            val airQualityRequest = async {
                airQualitySource.fetch(latitude, longitude, requestedTime)
            }
            weatherRequest.await() to airQualityRequest.await()
        }

        val result = EnvironmentalDataResult(
            weather = weather,
            aqis = airQuality,
            fetchTimestamp = fetchedAt
        )
        val sample = buildSample(latitude, longitude, result, requestedTime)
        if (sample == null) {
            Log.w("EnvFetcher", "neither weather nor valid air-quality data was returned")
        } else {
            Log.d("EnvFetcher", "sample quality=${sample.dataQuality}, source=${sample.source}")
        }
        return sample
    }

    private fun buildSample(
        latitude: Double,
        longitude: Double,
        result: EnvironmentalDataResult,
        requestedTime: Long
    ): EnvironmentalSample? {
        val weather = result.weather?.takeIf {
            kotlin.math.abs(it.timestamp - requestedTime) <= MAX_WEATHER_TIME_OFFSET_MS
        }
        val airQuality = result.aqis
        if (weather == null && airQuality == null) return null

        val sample = EnvironmentalSample(
            latitude = latitude,
            longitude = longitude,
            timestamp = airQuality?.timestamp ?: weather?.timestamp ?: result.fetchTimestamp,
            pm2_5 = airQuality?.pm2_5 ?: weather?.pm2_5,
            pm10 = airQuality?.pm10 ?: weather?.pm10,
            nitrogenDioxide = airQuality?.nitrogenDioxide ?: weather?.nitrogenDioxide,
            ozone = airQuality?.ozone ?: weather?.ozone,
            sulphurDioxide = airQuality?.sulphurDioxide ?: weather?.sulphurDioxide,
            carbonMonoxide = airQuality?.carbonMonoxide ?: weather?.carbonMonoxide,
            usAqi = airQuality?.usAqi,
            europeanAqi = airQuality?.europeanAqi ?: weather?.europeanAqi,
            temperatureCelsius = weather?.temperatureCelsius,
            apparentTemperatureCelsius = weather?.apparentTemperatureCelsius,
            relativeHumidityPercent = weather?.relativeHumidityPercent,
            windSpeedMs = weather?.windSpeedMs,
            windDirectionDegrees = weather?.windDirectionDegrees,
            precipitationMm = weather?.precipitationMm,
            rainMm = weather?.rainMm,
            cloudCoverPercent = weather?.cloudCoverPercent,
            uvIndex = weather?.uvIndex,
            source = if (airQuality != null) DataSource.OPEN_METEO_AQ else DataSource.OPEN_METEO,
            dataQuality = DataQuality.LOW,
            fetchedAt = result.fetchTimestamp
        )

        val hasAqi = sample.usAqi?.let { it in 0..500 } == true ||
            sample.europeanAqi?.let { it in 0..500 } == true
        val hasPollutant = sample.hasValidPollutantData()
        val quality = when {
            hasAqi && hasPollutant -> DataQuality.HIGH
            sample.hasValidAirQualityData() -> DataQuality.MEDIUM
            else -> DataQuality.LOW
        }

        return validator.validate(
            sample = sample.copy(dataQuality = quality),
            requestLat = latitude,
            requestLon = longitude,
            requestTime = requestedTime,
            now = result.fetchTimestamp
        )?.let { validatedQuality ->
            sample.copy(dataQuality = validatedQuality)
        }
    }

    private companion object {
        const val MAX_WEATHER_TIME_OFFSET_MS = 15 * 60 * 1_000L
    }
}
