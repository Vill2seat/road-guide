package com.roadguide.app.domain.model

/**
 * Географический контекст точки (Spec §8).
 *
 * OSM используется как географический контекст и дополнительный фактор,
 * а не как датчик фактической концентрации загрязнителя (§8.1).
 */
data class GeographicContext(
    val roadType: String? = null,        // motorway, trunk, primary, secondary, etc.
    val isGreenArea: Boolean = false,    // park, forest, grass, meadow, recreation_ground, garden
    val isIndustrial: Boolean = false,   // industrial zone nearby
    val isMajorRoad: Boolean = false,    // trunk, primary, motorway
    val nearbyFeatures: List<String> = emptyList(), // все совпавшие OSM теги
    val source: ContextSource = ContextSource.UNKNOWN
) {

    /** Преобразует контекст в строковый вид для ExposureCalculationEngine.calculateRoadFactor() */
    fun toRoadFactorContext(): String? {
        // Приоритет: industrial > major road > green area > road type
        if (isIndustrial) return "industrial"
        if (isMajorRoad) return roadType ?: "major_road"
        if (isGreenArea) return "green"
        return roadType
    }

    enum class ContextSource {
        OVERPASS,       // данные из Overpass API
        CACHE,          // данные из кэша
        FALLBACK,       // нейтральный контекст при недоступности сервиса
        UNKNOWN
    }
}