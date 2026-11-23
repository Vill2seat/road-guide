package com.roadguide.app.data.source

import android.util.Log
import com.roadguide.app.domain.model.RouteProfile
import com.roadguide.app.map.model.MapPoint
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.delay

/**
 * OSRM routing client (§12.2).
 *
 * Использует публичный OSRM сервер router.project-osrm.org.
 * Поддерживает альтернативные маршруты (alternatives=true) и профили
 * driving/walking/cycling.
 *
 * Замечания §12.2: OSRM поддерживает альтернативные маршруты, но не гарантирует
 * их наличие. При недоступности сервера возвращает null (graceful degradation).
 */
class OsrmRoutingSource(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String = "https://router.project-osrm.org",
    private val timeoutMs: Int = 30_000,
    private val maxRetries: Int = 3
) {

    private val TAG = "OsrmRoutingSource"

    /**
     * Запрашивает маршрут между двумя точками через OSRM.
     *
     * @param origin точка отправления
     * @param destination точка назначения
     * @param waypoints промежуточные точки (опционально)
     * @param profile профиль маршрутизации (driving/walking/cycling)
     * @return список альтернативных маршрутов или null при ошибке
     */
    suspend fun route(
        origin: MapPoint,
        destination: MapPoint,
        waypoints: List<MapPoint> = emptyList(),
        profile: RouteProfile = RouteProfile.DRIVING
    ): List<OsrmRoute>? {
        val allWaypoints = buildWaypoints(origin, waypoints, destination)
        val coords = allWaypoints.joinToString(";") { "${it.longitude},${it.latitude}" }

        val url = HttpUrl.Builder()
            .scheme("https")
            .host("router.project-osrm.org")
            .addPathSegment("route")
            .addPathSegment("v1")
            .addPathSegment("${profile.value}/$coords")
            .addQueryParameter("alternatives", "true")
            .addQueryParameter("geometries", "geojson")
            .addQueryParameter("overview", "full")
            .addQueryParameter("steps", "false")
            .build()

        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
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
                    val body = response.body?.string() ?: return null
                    return parseOsrmResponse(body)
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "OSRM attempt $attempt failed: ${e.message}")
                if (attempt < maxRetries) {
                    delay(1000L * attempt)
                }
            }
        }

        Log.w(TAG, "All OSRM attempts failed: ${lastException?.message}")
        return null
    }

    private fun buildWaypoints(
        origin: MapPoint,
        waypoints: List<MapPoint>,
        destination: MapPoint
    ): List<MapPoint> {
        val result = mutableListOf<MapPoint>()
        result.add(origin)
        result.addAll(waypoints)
        result.add(destination)
        return result
    }

    private fun parseOsrmResponse(json: String): List<OsrmRoute>? {
        try {
            val root = JSONObject(json)
            val code = root.opt("code")?.toString() ?: ""
            if (code != "Ok") {
                Log.w(TAG, "OSRM returned non-Ok code: $code")
                return null
            }

            val routes = root.optJSONArray("routes")
            val result = mutableListOf<OsrmRoute>()

            for (i in 0 until routes.length()) {
                val route = routes.getJSONObject(i)
                val distance = route.optDouble("distance", 0.0)
                val duration = route.optLong("duration", 0L)

                // Parse GeoJSON geometry
                val geometryObj = route.optJSONObject("geometry")
                val coordinates = geometryObj?.optJSONArray("coordinates") ?: continue
                val geometry = mutableListOf<MapPoint>()
                for (j in 0 until coordinates.length()) {
                    val coord = coordinates.optJSONArray(j)
                    val lon = coord.getDouble(0)
                    val lat = coord.getDouble(1)
                    geometry.add(MapPoint(lat, lon))
                }

                result.add(
                    OsrmRoute(
                        distanceMeters = distance,
                        durationSeconds = duration,
                        geometry = geometry.toList()
                    )
                )
            }

            return if (result.isEmpty()) null else result.toList()
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to parse OSRM response: ${e.message}")
            return null
        }
    }

    /** OSRM route result. */
    data class OsrmRoute(
        val distanceMeters: Double,
        val durationSeconds: Long,
        val geometry: List<MapPoint>
    )
}