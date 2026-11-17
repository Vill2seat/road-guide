package com.roadguide.app.data.source

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
    private val camsSource: CamsSource
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

        // Data quality validation
        val quality = validate(
            weather = weather,
            aqis = aqis,
            fetchedAt = fetchedAt,
            lat = lat,
            lon = lon
        )

        // Exposure level (0.0-1.0) derived from AQI for use in exposure formula
        // This is used by TrackingPipeline.calculateExposure as parameter P
        val exposureLevel = computeExposureLevel(europeanAqi, usAqi, pm2_5, pm10)

        return EnvironmentalSample(
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
            dataQuality = quality,
            modelResolutionKm = null,
            fetchedAt = fetchedAt
        )
    }

    private fun validate(
        weather: OpenMeteoWeather?,
        aqis: CamsAqis?,
        fetchedAt: Long,
        lat: Double,
        lon: Double
    ): DataQuality {
        // No data at all
        if (weather == null && aqis == null) {
            return DataQuality.UNKNOWN
        }

        // Check age
        val ageHours = (fetchedAt - (weather?.timestamp ?: aqis?.timestamp ?: fetchedAt)) / (1000.0 * 60 * 60)
        if (ageHours > ValidatorConstants.MAX_ENV_DATA_AGE_HOURS) {
            return DataQuality.STALE
        }

        // Check coordinate distance (30 km max)
        // We fetched exactly at lat/lon, so distance is 0 — always valid here.
        // This check matters when comparing against cached samples.

        // Check future offset
        val futureOffsetMinutes = (fetchedAt - System.currentTimeMillis()) / (1000.0 * 60)
        if (futureOffsetMinutes > ValidatorConstants.MAX_FUTURE_OFFSET_MINUTES) {
            return DataQuality.OUT_OF_RANGE
        }

        // Determine quality by data completeness
        val hasWeather = weather != null
        val hasAqis = aqis != null
        val hasPm = (weather?.pm2_5 != null || aqis?.pm2_5 != null) ||
            (weather?.pm10 != null || aqis?.pm10 != null)
        val hasAqi = weather?.europeanAqi != null || aqis?.usAqi != null

        return when {
            hasWeather && hasAqis && hasPm && hasAqi -> DataQuality.HIGH
            hasWeather && hasAqis -> DataQuality.MEDIUM
            hasWeather || hasAqis -> DataQuality.LOW
            else -> DataQuality.UNKNOWN
        }
    }

    /**
     * Computes a normalized exposure level (0.0-1.0) from AQI and particulate data.
     * Used as parameter P in E = K × P × T × A × R × W × S.
     */
    private fun computeExposureLevel(
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
}
