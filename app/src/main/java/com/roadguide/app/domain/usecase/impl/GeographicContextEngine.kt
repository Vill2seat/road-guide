package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.source.OverpassApiSource
import com.roadguide.app.domain.model.GeographicContext
import kotlin.math.roundToLong

class GeographicContextEngine(
    private val overpassSource: OverpassApiSource,
    private val cacheTtlMs: Long = 15 * 60 * 1000L,
    private val minRequestIntervalMs: Long = 1_000L
) {

    private val cache = mutableMapOf<String, CachedContext>()
    private var lastRequestTime = 0L

    suspend fun getContext(lat: Double, lon: Double): GeographicContext {
        val cacheKey = buildCacheKey(lat, lon)

        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
            return cached.context.copy(source = GeographicContext.ContextSource.CACHE)
        }

        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < minRequestIntervalMs) {
            kotlinx.coroutines.delay(minRequestIntervalMs - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()

        val elements = overpassSource.queryAround(lat, lon, radiusMeters = 500)

        val context = if (elements != null && elements.isNotEmpty()) {
            buildContextFromElements(elements)
        } else {
            GeographicContext(source = GeographicContext.ContextSource.FALLBACK)
        }

        cache[cacheKey] = CachedContext(context, System.currentTimeMillis())
        return context
    }

    suspend fun getRoadFactor(lat: Double, lon: Double): Double {
        val context = getContext(lat, lon)
        val contextStr = context.toRoadFactorContext()
        return ExposureCalculationEngine.calculateRoadFactor(contextStr)
    }

    private fun buildCacheKey(lat: Double, lon: Double): String {
        val latKey = (lat * 1000).roundToLong()
        val lonKey = (lon * 1000).roundToLong()
        return "${latKey}_${lonKey}"
    }

    private fun buildContextFromElements(elements: List<OverpassApiSource.OverpassElement>): GeographicContext {
        var roadType: String? = null
        var isGreenArea = false
        var isIndustrial = false
        var isMajorRoad = false
        val features = mutableListOf<String>()

        for (element in elements) {
            val tags = element.tags
            for ((key, value) in tags) {
                val tagLower = key.lowercase()
                val valLower = value.lowercase()

                if (tagLower == "highway") {
                    features.add("highway=$valLower")
                    roadType = valLower
                    when (valLower) {
                        "motorway", "trunk", "primary" -> isMajorRoad = true
                    }
                }

                if (tagLower == "landuse") {
                    features.add("landuse=$valLower")
                    when (valLower) {
                        "park", "forest", "grass", "meadow", "recreation_ground", "garden" ->
                            isGreenArea = true
                        "industrial" -> isIndustrial = true
                    }
                }

                if (tagLower == "leisure") {
                    features.add("leisure=$valLower")
                    when (valLower) {
                        "park", "garden", "recreation_ground" -> isGreenArea = true
                    }
                }

                if (tagLower == "natural") {
                    features.add("natural=$valLower")
                    when (valLower) {
                        "forest", "grassland", "meadow" -> isGreenArea = true
                    }
                }

                if (tagLower == "industrial") {
                    features.add("industrial=$valLower")
                    isIndustrial = true
                }

                if (tagLower == "amenity") {
                    features.add("amenity=$valLower")
                }

                if (tagLower == "building") {
                    features.add("building=$valLower")
                }

                if (tagLower == "railway") {
                    features.add("railway=$valLower")
                    isMajorRoad = true
                }

                if (tagLower == "public_transport") {
                    features.add("public_transport=$valLower")
                }
            }
        }

        return GeographicContext(
            roadType = roadType,
            isGreenArea = isGreenArea,
            isIndustrial = isIndustrial,
            isMajorRoad = isMajorRoad,
            nearbyFeatures = features.distinct(),
            source = GeographicContext.ContextSource.OVERPASS
        )
    }

    private data class CachedContext(
        val context: GeographicContext,
        val timestamp: Long
    )

    companion object {
        const val DEFAULT_RADIUS_METERS = 500
    }
}