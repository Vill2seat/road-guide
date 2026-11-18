package com.roadguide.app.data.source

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Open-Meteo weather and air quality client.
 * Endpoint: https://api.open-meteo.com/v1/forecast
 * No API key required.
 *
 * Features:
 * - Network connectivity check before each request
 * - Retry logic (3 attempts with 2 second delay)
 * - Explicit timeout handling
 * - Connection pooling via OkHttpClient
 */
class OpenMeteoSource(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val timeoutMs: Int = 10_000,
    private val maxRetries: Int = 3,
    private val retryDelayMs: Int = 2_000
) {

    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /**
     * Checks if network connectivity is available.
     */
    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    /**
     * Fetches current weather and air quality for given coordinates.
     * Returns null if network is unavailable or all retries fail.
     */
    suspend fun fetch(lat: Double, lon: Double): OpenMeteoWeather? {
        if (!isNetworkAvailable()) {
            return null
        }

        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return withTimeout(timeoutMs.toLong()) {
                    val url = buildUrl(lat, lon)
                    val response = httpGet(url)
                    parseOpenMeteo(response, lat, lon)
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delay(retryDelayMs.toLong())
                }
            }
        }
        return null
    }

    private fun buildUrl(lat: Double, lon: Double): String {
        val now = System.currentTimeMillis()
        val today = dateFormat.get().format(now)
        return "https://api.open-meteo.com/v1/forecast?" +
            "latitude=$lat&longitude=$lon&" +
            "current=temperature_2m,apparent_temperature,relative_humidity_2m," +
            "wind_speed_10m,wind_direction_10m,precipitation,rain,cloud_cover,uv_index&" +
            "hourly=pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide," +
            "carbon_monoxide,european_aqi&" +
            "timezone=UTC&start_date=$today&end_date=$today"
    }

    private fun httpGet(urlString: String): String {
        val request = Request.Builder()
            .url(urlString)
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw java.io.IOException("Unexpected code $response")
        }
        return response.body!!.string()
    }

    private fun parseOpenMeteo(json: String, lat: Double, lon: Double): OpenMeteoWeather? {
        // Simple JSON parsing without external library.
        // Values extracted by key lookup.
        val current = extractBlock(json, "\"current\":") ?: return null
        val hourly = extractBlock(json, "\"hourly\":") ?: return null

        val time = extractString(current, "\"time\"")
        val timestamp = if (time != null) dateFormat.get().parse(time)?.time ?: System.currentTimeMillis() else System.currentTimeMillis()

        return OpenMeteoWeather(
            temperatureCelsius = extractDouble(current, "\"temperature_2m\""),
            apparentTemperatureCelsius = extractDouble(current, "\"apparent_temperature\""),
            relativeHumidityPercent = extractDouble(current, "\"relative_humidity_2m\""),
            windSpeedMs = extractDouble(current, "\"wind_speed_10m\""),
            windDirectionDegrees = extractDouble(current, "\"wind_direction_10m\""),
            precipitationMm = extractDouble(current, "\"precipitation\""),
            rainMm = extractDouble(current, "\"rain\""),
            cloudCoverPercent = extractDouble(current, "\"cloud_cover\""),
            uvIndex = extractDouble(current, "\"uv_index\""),
            pm2_5 = extractFirstHourly(hourly, "\"pm2_5\""),
            pm10 = extractFirstHourly(hourly, "\"pm10\""),
            nitrogenDioxide = extractFirstHourly(hourly, "\"nitrogen_dioxide\""),
            ozone = extractFirstHourly(hourly, "\"ozone\""),
            sulphurDioxide = extractFirstHourly(hourly, "\"sulphur_dioxide\""),
            carbonMonoxide = extractFirstHourly(hourly, "\"carbon_monoxide\""),
            europeanAqi = extractFirstHourly(hourly, "\"european_aqi\"" )?.toInt(),
            timestamp = timestamp
        )
    }

    private fun extractBlock(json: String, key: String): String? {
        val start = json.indexOf(key)
        if (start == -1) return null
        val braceStart = json.indexOf('{', start)
        if (braceStart == -1) return null
        var depth = 0
        var i = braceStart
        while (i < json.length) {
            when (json[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return json.substring(braceStart, i + 1)
                }
            }
            i++
        }
        return null
    }

    private fun extractString(block: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
        val regex = pattern.toRegex()
        return regex.find(block)?.groupValues?.get(1)
    }

    private fun extractDouble(block: String, key: String): Double? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+(\\.\\d+)?)"
        val regex = pattern.toRegex()
        return regex.find(block)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractFirstHourly(hourly: String, key: String): Double? {
        // Find the first value array for the given key in hourly block
        val pattern = "\"$key\"\\s*:\\s*\\[([^\\]]*)\\]"
        val regex = pattern.toRegex()
        val match = regex.find(hourly) ?: return null
        val values = match.groupValues.get(1)
        val firstValue = values.split(",").firstOrNull()?.trim()?.toDoubleOrNull()
        return firstValue
    }
}
