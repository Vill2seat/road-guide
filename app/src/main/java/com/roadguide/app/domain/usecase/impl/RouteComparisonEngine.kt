package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.domain.model.RouteCategory
import com.roadguide.app.domain.model.RouteEvaluation
import kotlin.math.roundToInt

/**
 * RouteComparisonEngine — классифицирует и сравнивает маршруты (§12.4, §12.5).
 *
 * Классификация:
 * - FASTEST: маршрут с минимальной продолжительностью
 * - LOWEST_EXPOSURE: маршрут с минимальным exposureScore (только если confidence >= 0.3 и coverage >= 30%)
 * - BALANCED: компромисс между временем и экологией
 *
 * Spec §12.5: Не выбирать самый экологичный маршрут, если данные по нему недостаточны,
 * без предупреждения (confidence < 0.3 или coverage < 30%).
 */
class RouteComparisonEngine {

    companion object {
        private const val MIN_CONFIDENCE_FOR_ECO = 0.3
        private const val MIN_COVERAGE_FOR_ECO = 30.0
    }

    /**
     * Классифицирует список маршрутов по категориям.
     *
     * Каждому маршруту присваивается категория:
     * - один маршрут получает FASTEST (минимум duration)
     * - один маршрут получает LOWEST_EXPOSURE (минимум exposureScore, если проходит пороги)
     * - один маршрут получает BALANCED (лучший компромисс)
     * - остальные остаются без категории (или могут быть переклассифицированы)
     */
    fun classifyRoutes(routes: List<RouteEvaluation>): List<RouteEvaluation> {
        if (routes.isEmpty()) return emptyList()

        val result = routes.toMutableList()

        // FASTEST: маршрут с минимальной продолжительностью
        val fastest = result.minByOrNull { it.durationSeconds }!!
        result.find { it.id == fastest.id }?.category = RouteCategory.FASTEST

        // LOWEST_EXPOSURE: только если есть маршрут с достаточными данными
        val eligibleForEco = result.filter { r ->
            r.hasAirQualityData &&
                r.confidence >= MIN_CONFIDENCE_FOR_ECO &&
                r.coverage >= MIN_COVERAGE_FOR_ECO
        }
        if (eligibleForEco.isNotEmpty()) {
            val lowestExposure = eligibleForEco.minByOrNull { it.exposureScore }!!
            result.find { it.id == lowestExposure.id }?.category = RouteCategory.LOWEST_EXPOSURE
        }

        // BALANCED: компромисс между нормализованным временем и экспозицией
        val balanced = selectBalanced(result)
        if (balanced != null) {
            result.find { it.id == balanced.id }?.category = RouteCategory.BALANCED
        }

        return result
    }

    /**
     * Выбирает лучший маршрут для рекомендации пользователю (§12.5).
     *
     * Приоритет:
     * 1. LOWEST_EXPOSURE (если есть и проходит пороги данных)
     * 2. BALANCED
     * 3. FASTEST
     *
     * Возвращает null, если нет подходящих маршрутов.
     */
    fun selectBestRoute(routes: List<RouteEvaluation>): RouteEvaluation? {
        if (routes.isEmpty()) return null

        // Предпочитаем экологичный маршрут только если данные достаточны
        val ecoRoute = routes.firstOrNull { r ->
            r.category == RouteCategory.LOWEST_EXPOSURE &&
                r.hasAirQualityData &&
                r.confidence >= MIN_CONFIDENCE_FOR_ECO &&
                r.coverage >= MIN_COVERAGE_FOR_ECO
        }
        if (ecoRoute != null) return ecoRoute

        val balanced = routes.firstOrNull { r -> r.category == RouteCategory.BALANCED }
        if (balanced != null) return balanced

        return routes.firstOrNull { r -> r.category == RouteCategory.FASTEST }
    }

    private fun selectBalanced(routes: List<RouteEvaluation>): RouteEvaluation? {
        if (routes.isEmpty()) return null

        val routesWithAirQuality = routes.filter { it.hasAirQualityData }
        if (routesWithAirQuality.isEmpty()) return null

        val maxDuration = routesWithAirQuality.maxOfOrNull { it.durationSeconds.toDouble() } ?: 1.0
        val maxExposure = routesWithAirQuality.maxOfOrNull { it.exposureScore } ?: 100.0

        return routesWithAirQuality.minByOrNull { route ->
            // Нормализованный скор: чем меньше время и экспозиция, тем лучше
            val timeNorm = route.durationSeconds.toDouble() / maxDuration
            val exposureNorm = route.exposureScore / maxExposure
            // Взвешенная сумма: 50% время, 50% экспозиция
            0.5 * timeNorm + 0.5 * exposureNorm
        }
    }

    /**
     * Проверяет, можно ли рекомендовать экологичный маршрут (§12.5).
     * Возвращает предупреждающее сообщение если данные недостаточны.
     */
    fun getEcoRouteWarning(routes: List<RouteEvaluation>): String? {
        val ecoRoutes = routes.filter { it.category == RouteCategory.LOWEST_EXPOSURE }
        if (ecoRoutes.isEmpty()) return null

        val insufficient = ecoRoutes.filter { r ->
            !r.hasAirQualityData ||
                r.confidence < MIN_CONFIDENCE_FOR_ECO ||
                r.coverage < MIN_COVERAGE_FOR_ECO
        }
        if (insufficient.isNotEmpty()) {
            return "Маршрут с наименьшей экспозицией имеет недостаточные данные (уверенность: ${(insufficient.first().confidence * 100).roundToInt()}%, покрытие: ${insufficient.first().coverage.roundToInt()}%). Рекомендуется использовать альтернативный маршрут."
        }
        return null
    }
}