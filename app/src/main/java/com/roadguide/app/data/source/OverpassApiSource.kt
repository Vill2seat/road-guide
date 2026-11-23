package com.roadguide.app.data.source

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.delay

class OverpassApiSource(
    private val okHttpClient: OkHttpClient,
    private val timeoutMs: Int = 25_000,
    private val maxRetries: Int = 3
) {

    private val TAG = "OverpassApiSource"

    private val endpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.nchc.org.tw/api/interpreter"
    )

    suspend fun queryAround(
        lat: Double,
        lon: Double,
        radiusMeters: Int = 500
    ): List<OverpassElement>? {
        val query = buildOverpassQuery(lat, lon, radiusMeters)

        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            for (endpoint in endpoints) {
                try {
                    val response = executeQuery(endpoint, query)
                    if (response != null) {
                        Log.d(TAG, "Overpass query succeeded: $endpoint, elements=${response.size}")
                        return response
                    }
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Overpass query attempt $attempt failed for $endpoint: ${e.message}")
                }
            }
            if (attempt < maxRetries) {
                delay(1000L * attempt)
            }
        }

        Log.w(TAG, "All Overpass attempts failed: ${lastException?.message}")
        return null
    }

    private suspend fun executeQuery(endpoint: String, query: String): List<OverpassElement>? {
        val body = RequestBody.create(
            "application/x-www-form-urlencoded".toMediaTypeOrNull(),
            "data=$query".toByteArray()
        )
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        val clientWithTimeout = OkHttpClient.Builder()
            .connectTimeout(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        clientWithTimeout.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val responseBody = response.body?.string() ?: return null
            return parseOverpassResponse(responseBody)
        }
    }

    private fun buildOverpassQuery(lat: Double, lon: Double, radiusMeters: Int): String {
        return """
            [out:json][timeout:25];
            (
              node["highway"](around:$radiusMeters,$lat,$lon);
              way["highway"](around:$radiusMeters,$lat,$lon);
              node["landuse"](around:$radiusMeters,$lat,$lon);
              way["landuse"](around:$radiusMeters,$lat,$lon);
              node["leisure"](around:$radiusMeters,$lat,$lon);
              way["leisure"](around:$radiusMeters,$lat,$lon);
              node["natural"](around:$radiusMeters,$lat,$lon);
              way["natural"](around:$radiusMeters,$lat,$lon);
              node["industrial"](around:$radiusMeters,$lat,$lon);
              way["industrial"](around:$radiusMeters,$lat,$lon);
              node["amenity"](around:$radiusMeters,$lat,$lon);
              way["amenity"](around:$radiusMeters,$lat,$lon);
              node["building"](around:$radiusMeters,$lat,$lon);
              way["building"](around:$radiusMeters,$lat,$lon);
              node["railway"](around:$radiusMeters,$lat,$lon);
              way["railway"](around:$radiusMeters,$lat,$lon);
              node["public_transport"](around:$radiusMeters,$lat,$lon);
              way["public_transport"](around:$radiusMeters,$lat,$lon);
            );
            out center tags;
        """.trimIndent()
    }

    private fun parseOverpassResponse(json: String): List<OverpassElement>? {
        try {
            val root = JSONObject(json)
            val elements = root.optJSONArray("elements")
            val result = mutableListOf<OverpassElement>()
            for (i in 0 until elements.length()) {
                val elem = elements.optJSONObject(i)
                val type = elem.getString("type")
                val id = elem.getLong("id")
                val tags = if (elem.has("tags")) elem.optJSONObject("tags") else JSONObject()
                val lat = elem.optDouble("lat", Double.NaN)
                val lon = elem.optDouble("lon", Double.NaN)
val centerLat = if (elem.has("center")) elem.optJSONObject("center").optDouble("lat", Double.NaN) else lat
        val centerLon = if (elem.has("center")) elem.optJSONObject("center").optDouble("lon", Double.NaN) else lon

                val tagsMap = mutableMapOf<String, String>()
            val keysIter = tags.keys()
            while (keysIter.hasNext()) {
                val key = keysIter.next()
                tagsMap[key] = tags.getString(key)
            }

                result.add(
                    OverpassElement(
                        type = type,
                        id = id,
                        tags = tagsMap,
                        lat = if (lat.isNaN()) null else lat,
                        lon = if (lon.isNaN()) null else lon,
                        centerLat = if (centerLat.isNaN()) null else centerLat,
                        centerLon = if (centerLon.isNaN()) null else centerLon
                    )
                )
            }
            return result
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to parse Overpass response: ${e.message}")
            return null
        }
    }

    data class OverpassElement(
        val type: String,
        val id: Long,
        val tags: Map<String, String>,
        val lat: Double?,
        val lon: Double?,
        val centerLat: Double?,
        val centerLon: Double?
    )
}