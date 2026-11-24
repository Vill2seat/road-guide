package com.roadguide.app.data.source

import android.util.Log
import com.roadguide.app.domain.model.RouteProfile
import com.roadguide.app.map.model.MapPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
    private val baseHttpUrl = baseUrl.toHttpUrl()
    private val client = okHttpClient.newBuilder()
        .callTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        .build()

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
        if (allWaypoints.any { !it.isValidCoordinate() }) {
            Log.w(TAG, "route request contains invalid coordinates")
            return null
        }
        val coords = allWaypoints.joinToString(";") { "${it.longitude},${it.latitude}" }

        val url = baseHttpUrl.newBuilder()
            .encodedPath("${baseHttpUrl.encodedPath.trimEnd('/')}/route/v1")
            .addPathSegment(profile.value)
            .addPathSegment(coords)
            .addQueryParameter("alternatives", "true")
            .addQueryParameter("geometries", "geojson")
            .addQueryParameter("overview", "full")
            .addQueryParameter("steps", "false")
            .build()

        var lastException: IOException? = null
        for (attempt in 1..maxRetries) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val body = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("HTTP ${response.code}")
                        }
                        response.body?.string()
                            ?: throw IOException("OSRM returned an empty response")
                    }
                }
                return parseOsrmResponse(body)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
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

    private fun MapPoint.isValidCoordinate(): Boolean =
        latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0

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

            val routes = root.optJSONArray("routes") ?: return null
            val result = mutableListOf<OsrmRoute>()

            for (i in 0 until routes.length()) {
                val route = routes.getJSONObject(i)
                val distance = route.optDouble("distance", Double.NaN)
                val duration = route.optDouble("duration", Double.NaN)
                if (!distance.isFinite() || distance < 0.0 ||
                    !duration.isFinite() || duration < 0.0
                ) continue

                // Parse GeoJSON geometry
                val geometryObj = route.optJSONObject("geometry")
                val coordinates = geometryObj?.optJSONArray("coordinates") ?: continue
                val geometry = mutableListOf<MapPoint>()
                for (j in 0 until coordinates.length()) {
                    val coord = coordinates.optJSONArray(j) ?: continue
                    if (coord.length() < 2) continue
                    val lon = coord.optDouble(0, Double.NaN)
                    val lat = coord.optDouble(1, Double.NaN)
                    val point = MapPoint(lat, lon)
                    if (point.isValidCoordinate()) geometry.add(point)
                }
                if (geometry.size < 2) continue

                result.add(
                    OsrmRoute(
                        distanceMeters = distance,
                        durationSeconds = duration.toLong(),
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