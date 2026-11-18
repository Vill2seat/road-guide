package com.roadguide.app.data.source

import com.roadguide.app.data.local.EnvironmentalValidator
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.DataSource
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.ValidatorConstants

/**
 * Orchestrates fetching environmental data from multiple sources,
 * aggregates into a single EnvironmentalSample, and applies validation.
 */
class EnvironmentalDataFetcher(
    private val openMeteoSource: OpenMeteoSource,
    private val camsSource: CamsSource,
    private val validator: EnvironmentalValidator
) {

    /**
     * Fetches weather and AQI for given coordinates, merges sources,
     * validates quality, and returns an EnvironmentalSample ready for persistence.
     */
    suspend fun fetch(lat: Double, lon: Double): EnvironmentalSample? {
        val now = System.currentTimeMillis()

        val weather = openMeteoSource.fetch(lat, lon)
        val aqis = camsSource.fetch(lat, lon)

        val result = EnvironmentalDataResult(
            weather = weather,
            aqis = aqis,
            fetchTimestamp = now
        )

        return buildSample(lat, lon, result, now)
    }

    private fun buildSample(
        lat: Double,
        lon: Double,
        result: EnvironmentalDataResult,
        fetchedAt: Long
    ): EnvironmentalSample? {
        val weather = result.weather
        val aqis = result.aqis

        // Determine primary source
        val source = when {
            weather != null && aqis != null -> DataSource.OPEN_METEO
            weather != null -> DataSource.OPEN_METEO
            aqis != null -> DataSource.CAMS
            else -> return null
        }

        // Merge PM2.5/PM10 from CAMS if Open-Meteo missing them
        val pm2_5 = weather?.pm2_5 ?: aqis?.pm2_5
        val pm10 = weather?.pm10 ?: aqis?.pm10
        val nitrogenDioxide = weather?.nitrogenDioxide ?: aqis?.nitrogenDioxide
        val ozone = weather?.ozone ?: aqis?.ozone
        val sulphurDioxide = weather?.sulphurDioxide ?: aqis?.sulphurDioxide
        val carbonMonoxide = weather?.carbonMonoxide ?: aqis?.carbonMonoxide

        // AQI: prefer European AQI from Open-Meteo, fallback to US AQI from CAMS
        val europeanAqi = weather?.europeanAqi
        val usAqi = aqis?.usAqi

        // Preliminary quality based on source completeness
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

        // Exposure level (0.0-1.0) derived from AQI for use in exposure formula
        val exposureLevel = calculateExposureLevel(europeanAqi, usAqi, pm2_5, pm10)

        val sample = EnvironmentalSample(
            latitude = lat,
            longitude = lon,
            timestamp = fetchedAt,
            pm2_5 = pm2_5,
            pm10 = pm10,
            nitrogenDioxide = nitrogenDioxide,
            ozone = ozone,
            sulphurDioxide = sulphurDioxide,
            carbonMonoxide = carbonMonoxide,
            usAqi = usAqi,
            europeanAqi = europeanAqi,
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

        // Validate with injected EnvironmentalValidator (single source of truth for validation)
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

    /**
     * Computes a normalized exposure level (0.0-1.0) from AQI and particulate data.
     * Used as parameter P in E = K × P × T × A × R × W × S.
     */
    fun computeExposureLevel(
        europeanAqi: Int?,
        usAqi: Int?,
        pm2_5: Double?,
        pm10: Double?
    ): Double {
        return calculateExposureLevel(europeanAqi, usAqi, pm2_5, pm10)
    }
}

/**
 * Top-level exposure level calculation shared between
 * [EnvironmentalDataFetcher] and exposure calculation logic.
 */
fun calculateExposureLevel(
    europeanAqi: Int?,
    usAqi: Int?,
    pm2_5: Double?,
    pm10: Double?
): Double {
    val aqi = europeanAqi ?: usAqi ?: return 0.0

    // Normalize AQI to 0.0-1.0 range (AQI 500 = 1.0 max exposure)
    val aqiLevel = (aqi.coerceIn(0, 500) / 500.0)

    // Adjust with PM2.5 if available (WHO guideline: 25 µg/m³ 24h mean = significant)
    val pmAdjustment = when {
        pm2_5 != null -> pm2_5.coerceIn(0.0, 100.0) / 100.0
        else -> 0.0
    }

    return (aqiLevel * 0.7 + pmAdjustment * 0.3).coerceIn(0.0, 1.0)
}
