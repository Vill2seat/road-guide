package com.roadguide.app.data.source

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.format.DateTimeFormatter

class OpenMeteoAirQualitySource(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1_000L
) {

    suspend fun fetch(
        latitude: Double,
        longitude: Double,
        requestedTime: Long = System.currentTimeMillis()
    ): OpenMeteoAirQuality? {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            Log.w(TAG, "invalid coordinates")
            return null
        }
        if (!isNetworkAvailable()) {
            Log.w(TAG, "network unavailable")
            return null
        }

        var lastException: IOException? = null
        repeat(maxRetries) { attempt ->
            try {
                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(buildUrl(latitude, longitude, requestedTime))
                        .header("Accept", "application/json")
                        .build()
                    okHttpClient.newCall(request).execute().use { result ->
                        if (!result.isSuccessful) {
                            throw IOException("Open-Meteo Air Quality HTTP ${result.code}")
                        }
                        result.body?.string()
                            ?: throw IOException("Open-Meteo Air Quality returned an empty body")
                    }
                }
                return parseResponse(response, requestedTime).also { parsed ->
                    if (parsed == null) Log.w(TAG, "response contained no valid air-quality values")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries - 1) delay(retryDelayMs)
            }
        }

        Log.e(TAG, "request failed after $maxRetries attempts", lastException)
        return null
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.activeNetworkInfo?.isConnected == true
    }

    private fun buildUrl(latitude: Double, longitude: Double, requestedTime: Long): String {
        val date = Instant.ofEpochMilli(requestedTime)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "https://air-quality-api.open-meteo.com/v1/air-quality?" +
            "latitude=$latitude&longitude=$longitude&" +
            "hourly=pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide,us_aqi,european_aqi&" +
            "timezone=UTC&start_date=$date&end_date=$date"
    }

    companion object {
        private const val TAG = "OpenMeteoAirQuality"
        private const val MAX_CONCENTRATION = 100_000.0
        private const val MAX_TIME_OFFSET_MS = 30 * 60 * 1_000L

        internal fun parseResponse(json: String, requestedTime: Long): OpenMeteoAirQuality? {
            val hourly = extractObject(json, "hourly") ?: return null
            val times = extractStringArray(hourly, "time") ?: return null
            val candidates = times.mapIndexedNotNull { index, value ->
                val timestamp = try {
                    LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
                } catch (_: DateTimeParseException) {
                    null
                } ?: return@mapIndexedNotNull null

                val offset = timestamp - requestedTime
                if (kotlin.math.abs(offset) <= MAX_TIME_OFFSET_MS) index to timestamp else null
            }
            val (index, timestamp) = candidates.minByOrNull {
                kotlin.math.abs(it.second - requestedTime)
            } ?: return null

            val result = OpenMeteoAirQuality(
                usAqi = extractNumberAt(hourly, "us_aqi", index)?.toIntExact()?.takeIf { it in 0..500 },
                europeanAqi = extractNumberAt(hourly, "european_aqi", index)
                    ?.toIntExact()
                    ?.takeIf { it in 0..500 },
                pm2_5 = extractConcentrationAt(hourly, "pm2_5", index),
                pm10 = extractConcentrationAt(hourly, "pm10", index),
                nitrogenDioxide = extractConcentrationAt(hourly, "nitrogen_dioxide", index),
                ozone = extractConcentrationAt(hourly, "ozone", index),
                sulphurDioxide = extractConcentrationAt(hourly, "sulphur_dioxide", index),
                carbonMonoxide = extractConcentrationAt(hourly, "carbon_monoxide", index),
                timestamp = timestamp
            )
            return result.takeIf {
                it.usAqi != null || it.europeanAqi != null ||
                    it.pm2_5 != null || it.pm10 != null ||
                    it.nitrogenDioxide != null || it.ozone != null ||
                    it.sulphurDioxide != null || it.carbonMonoxide != null
            }
        }

        private fun extractObject(json: String, key: String): String? {
            val keyPosition = json.indexOf("\"$key\"")
            if (keyPosition < 0) return null
            val objectStart = json.indexOf('{', keyPosition)
            if (objectStart < 0) return null

            var depth = 0
            for (index in objectStart until json.length) {
                when (json[index]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return json.substring(objectStart, index + 1)
                    }
                }
            }
            return null
        }

        private fun extractStringArray(json: String, key: String): List<String>? {
            val contents = extractArray(json, key) ?: return null
            return Regex(""""([^"]*)"""")
                .findAll(contents)
                .map { it.groupValues[1] }
                .toList()
        }

        private fun extractArray(json: String, key: String): String? =
            Regex(""""$key"\s*:\s*\[([^\]]*)\]""")
                .find(json)
                ?.groupValues
                ?.get(1)

        private fun extractNumberAt(json: String, key: String, index: Int): Double? =
            extractArray(json, key)
                ?.split(',')
                ?.getOrNull(index)
                ?.trim()
                ?.takeUnless { it == "null" }
                ?.toDoubleOrNull()

        private fun extractConcentrationAt(json: String, key: String, index: Int): Double? {
            val number = extractNumberAt(json, key, index) ?: return null
            return number.takeIf { it.isFinite() && it in 0.0..MAX_CONCENTRATION }
        }

        private fun Double.toIntExact(): Int? =
            takeIf { isFinite() && this % 1.0 == 0.0 }?.toInt()
    }
}
