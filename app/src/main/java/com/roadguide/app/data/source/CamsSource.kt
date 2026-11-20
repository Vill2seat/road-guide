package com.roadguide.app.data.source

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * CAMS (Copernicus Atmosphere Monitoring Service) air quality client.
 * Requires a valid API key for production use.
 *
 * Features:
 * - Network connectivity check before each request
 * - Retry logic (3 attempts with 2 second delay)
 * - Explicit timeout handling
 * - Connection pooling via OkHttpClient
 */
class CamsSource(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val apiKey: String? = null,
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
     * Fetches AQI and atmospheric composition for given coordinates.
     * Returns null if API key is missing, network is unavailable, or all retries fail.
     */
    suspend fun fetch(lat: Double, lon: Double): CamsAqis? {
        if (apiKey == null || apiKey.isBlank()) {
            Log.w("CamsSource", "api key missing")
            return null
        }

        if (!isNetworkAvailable()) {
            Log.w("CamsSource", "network unavailable")
            return null
        }

        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return withTimeout(timeoutMs.toLong()) {
                    val url = buildUrl(lat, lon)
                    val response = httpGet(url)
                    parseCams(response)
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delay(retryDelayMs.toLong())
                }
            }
        }
        Log.e("CamsSource", "failed after $maxRetries attempts for lat=$lat, lon=$lon", lastException)
        return null
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
        val request = Request.Builder()
            .url(urlString)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e("CamsSource", "HTTP ${response.code}: ${response.message}")
            throw java.io.IOException("Unexpected code ${response.code}")
        }
        return response.body!!.string()
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
