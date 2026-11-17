package com.roadguide.app.data.source

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

/**
 * CAMS (Copernicus Atmosphere Monitoring Service) air quality client.
 * Requires a valid API key for production use.
 */
class CamsSource(
    private val apiKey: String? = null,
    private val timeoutMs: Int = 10_000
) {

    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /**
     * Fetches AQI and atmospheric composition for given coordinates.
     * Returns null if API key is missing or request fails.
     */
    suspend fun fetch(lat: Double, lon: Double): CamsAqis? {
        if (apiKey == null || apiKey.isBlank()) {
            return null
        }
        return try {
            val url = buildUrl(lat, lon)
            val response = httpGet(url)
            parseCams(response)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildUrl(lat: Double, lon: Double): String {
        val today = dateFormat.get().format(System.currentTimeMillis())
        return "https://api.atmosphere.copernicus.eu/v1/forecast?" +
            "latitude=$lat&longitude=$lon&" +
            "variable=pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide,us_aqi&" +
            "start_date=$today&end_date=$today&" +
            "apikey=$apiKey"
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.addRequestProperty("Authorization", "Bearer $apiKey")
        connection.instanceFollowRedirects = true

        val input: InputStream
        if (connection.responseCode >= 400) {
            input = connection.errorStream
        } else {
            input = connection.inputStream
        }

        return input.bufferedReader().use { it.readText() }
    }

    private fun parseCams(json: String): CamsAqis? {
        val usAqi = extractInt(json, "\"us_aqi\"")
        val pm2_5 = extractDouble(json, "\"pm2_5\"")
        val pm10 = extractDouble(json, "\"pm10\"")
        val nitrogenDioxide = extractDouble(json, "\"nitrogen_dioxide\"")
        val ozone = extractDouble(json, "\"ozone\"")
        val sulphurDioxide = extractDouble(json, "\"sulphur_dioxide\"")
        val carbonMonoxide = extractDouble(json, "\"carbon_monoxide\"")
        val timestamp = System.currentTimeMillis()

        return CamsAqis(
            usAqi = usAqi,
            pm2_5 = pm2_5,
            pm10 = pm10,
            nitrogenDioxide = nitrogenDioxide,
            ozone = ozone,
            sulphurDioxide = sulphurDioxide,
            carbonMonoxide = carbonMonoxide,
            timestamp = timestamp
        )
    }

    private fun extractInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+)"
        val regex = pattern.toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractDouble(json: String, key: String): Double? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+(\\.\\d+)?)"
        val regex = pattern.toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
