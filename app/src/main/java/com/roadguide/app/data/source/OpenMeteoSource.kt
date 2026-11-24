package com.roadguide.app.data.source

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext

/** Open-Meteo weather client (current weather + hourly forecast). */
class OpenMeteoWeatherSource(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val timeoutMs: Int = 10_000,
    private val maxRetries: Int = 3,
    private val retryDelayMs: Int = 2_000
) {

    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private val timestampFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private val TAG = "OpenMeteoWeatherSource"

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    private suspend fun delayMs(ms: Long) {
        kotlinx.coroutines.delay(ms)
    }

    /** Fetches current weather and hourly forecast. */
    suspend fun fetch(lat: Double, lon: Double): OpenMeteoWeather? {
        if (!isNetworkAvailable()) {
            Log.w("OpenMeteoWeatherSource", "network unavailable")
            return null
        }

        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return withTimeout(timeoutMs.toLong()) {
                    val response = withContext(Dispatchers.IO) {
                        httpGet(buildUrl(lat, lon))
                    }
                    parseOpenMeteo(response, lat, lon)
                }
            } catch (e: TimeoutCancellationException) {
                lastException = e
                if (attempt < maxRetries) delayMs(retryDelayMs.toLong())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) delayMs(retryDelayMs.toLong())
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delayMs(retryDelayMs.toLong())
                }
            }
        }
        Log.e(TAG, "failed after $maxRetries attempts for lat=$lat, lon=$lon", lastException)
        return null
    }

    private fun buildUrl(lat: Double, lon: Double): String {
        val now = System.currentTimeMillis()
        val today = dateFormat.get().format(now)
        return "https://api.open-meteo.com/v1/forecast?" +
            "latitude=$lat&longitude=$lon&" +
            "current=temperature_2m,apparent_temperature,relative_humidity_2m," +
            "wind_speed_10m,wind_direction_10m,precipitation,rain,cloud_cover,uv_index&" +
            "hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,precipitation,rain,cloud_cover,uv_index&" +
            "wind_speed_unit=ms&" +
            "timezone=UTC&start_date=$today&end_date=$today"
    }

    private fun httpGet(urlString: String): String {
        val request = Request.Builder().url(urlString).build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP ${response.code}: ${response.message}")
                throw IOException("Unexpected code ${response.code}")
            }
            response.body?.string() ?: throw IOException("Empty weather response body")
        }
    }

    private fun parseOpenMeteo(json: String, lat: Double, lon: Double): OpenMeteoWeather? {
        val current = extractBlock(json, "\"current\"") ?: return null
        val hourly = extractBlock(json, "\"hourly\"") ?: return null

        val time = extractString(current, "\"time\"")
            ?: extractString(hourly, "time")
        val ts = if (time != null) {
            timestampFormat.get().parse(time)?.time ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }

        return try {
            OpenMeteoWeather(
                temperatureCelsius = extractDouble(current, "\"temperature_2m\"")!!,
                apparentTemperatureCelsius = extractDouble(current, "\"apparent_temperature\"")!!,
                relativeHumidityPercent = extractDouble(current, "\"relative_humidity_2m\"")!!,
                windSpeedMs = extractDouble(current, "\"wind_speed_10m\"")!!,
                windDirectionDegrees = extractDouble(current, "\"wind_direction_10m\"")!!,
                precipitationMm = extractDouble(current, "\"precipitation\"")!!,
                rainMm = extractDouble(current, "\"rain\"")!!,
                cloudCoverPercent = extractDouble(current, "\"cloud_cover\"")!!,
                uvIndex = extractDouble(current, "\"uv_index\"")!!,
                pm2_5 = extractDouble(current, "\"pm2_5\""),
                pm10 = extractDouble(current, "\"pm10\""),
                nitrogenDioxide = extractDouble(current, "\"nitrogen_dioxide\""),
                ozone = extractDouble(current, "\"ozone\""),
                sulphurDioxide = extractDouble(current, "\"sulphur_dioxide\""),
                carbonMonoxide = extractDouble(current, "\"carbon_monoxide\""),
                europeanAqi = extractInt(current, "\"european_aqi\""),
                timestamp = ts
            )
        } catch (e: Exception) {
            Log.e(TAG, "parse failed for lat=$lat, lon=$lon", e)
            null
        }
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

    private fun extractInt(block: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(-?\\d+)"
        val regex = pattern.toRegex()
        return regex.find(block)?.groupValues?.get(1)?.toIntOrNull()
    }
}