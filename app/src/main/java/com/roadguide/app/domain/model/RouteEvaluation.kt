package com.roadguide.app.domain.model

import com.roadguide.app.map.model.MapPoint

/**
 * Результат оценки маршрута (§12.4).
 */
data class RouteEvaluation(
    val id: String,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val geometry: List<MapPoint>,
    val exposureScore: Double,    // 0-100 (ниже = лучше)
    val confidence: Double,       // 0-1
    val coverage: Double,         // 0-100%
    val pollutionContext: String?,
    val roadProximity: Double,    // 0-1 (выше = ближе к дорогам)
    val greenAreaProximity: Double, // 0-1 (выше = ближе к зеленым зонам)
    var category: RouteCategory,
    val summary: String
)

/** Категория маршрута для отображения пользователю (§12.5). */
enum class RouteCategory {
    FASTEST,        // минимальное время
    LOWEST_EXPOSURE, // минимальная экспозиция
    BALANCED        // компромисс между временем и экологией
}

/** Профиль маршрутизации (§12.3). */
enum class RouteProfile(val value: String) {
    DRIVING("driving"),
    WALKING("walking"),
    CYCLING("cycling")
}