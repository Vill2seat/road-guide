package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.local.entity.DailyReport
import com.roadguide.app.data.local.entity.ExposureResult
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.data.local.entity.Stay
import com.roadguide.app.data.repository.DailyReportRepository
import com.roadguide.app.data.repository.ExposureResultRepository
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.PrivacyZoneRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.repository.RouteSegmentRepository
import com.roadguide.app.data.repository.StayRepository
import com.roadguide.app.domain.usecase.AggregateDailyExposure
import com.roadguide.app.domain.usecase.impl.PrivacyScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlin.math.roundToLong

class DailyExposureAggregator(
    private val routeRepository: RouteRepository,
    private val routeSegmentRepository: RouteSegmentRepository,
    private val exposureResultRepository: ExposureResultRepository,
    private val stayRepository: StayRepository,
    private val dailyReportRepository: DailyReportRepository,
    private val locationPointRepository: LocationPointRepository,
    private val privacyZoneRepository: PrivacyZoneRepository,
) : AggregateDailyExposure {

    companion object {
        internal const val RAW_SCORE_CAP: Double = 200.0
    }

    override suspend fun aggregate(date: String): DailyReport? {
        return buildReport(date, isRegenerated = false)
    }

    private suspend fun buildReport(date: String, isRegenerated: Boolean): DailyReport? {
        val routes = routeRepository.getByDate(date).take(1).firstOrNull()
        if (routes.isNullOrEmpty()) {
            dailyReportRepository.deleteByDate(date)
            return null
        }

        val routeIds = routes.map { it.id }.toSet()
        val activeZones = privacyZoneRepository.observeActiveZones().take(1).firstOrNull() ?: emptyList()

        val allResults = exposureResultRepository.observeAll().take(1).firstOrNull() ?: emptyList()
        val allSegments = routeIds.flatMap { routeId ->
            routeSegmentRepository.observeByRoute(routeId).take(1).firstOrNull() ?: emptyList()
        }
        val pointsByRoute = routeIds.associateWith { routeId ->
            locationPointRepository.observeByRoute(routeId).take(1).firstOrNull() ?: emptyList()
        }
        val allStays = routeIds.flatMap { routeId ->
            stayRepository.observeByRoute(routeId).take(1).firstOrNull() ?: emptyList()
        }

        val historySegments = allSegments.filterNot { segment ->
            PrivacyZonePolicy.excludesSegment(
                segment,
                pointsByRoute[segment.routeId] ?: emptyList(),
                activeZones,
                PrivacyScope.HISTORY
            )
        }
        val exposureSegments = historySegments.filterNot { segment ->
            PrivacyZonePolicy.excludesSegment(
                segment,
                pointsByRoute[segment.routeId] ?: emptyList(),
                activeZones,
                PrivacyScope.EXPOSURE
            )
        }
        val exposureSegmentIds = exposureSegments.mapTo(mutableSetOf()) { it.id }
        val routeResults = allResults.filter { result ->
            result.routeId?.let(routeIds::contains) == true &&
                result.segmentId != null &&
                result.segmentId in exposureSegmentIds
        }
        if (routeResults.isEmpty()) {
            dailyReportRepository.deleteByDate(date)
            return null
        }

        val visibleStays = allStays.filterNot { stay ->
            PrivacyZonePolicy.excludesStay(stay, activeZones, PrivacyScope.HISTORY)
        }

        var walkingDuration = 0L
        var transportDuration = 0L
        var stationaryDuration = 0L
        var totalDistance = 0.0
        var totalDurationSeconds = 0L

        val hasHistoryExclusion = activeZones.any { it.isActive && it.excludeFromHistory }
        for (route in routes) {
            val routePoints = pointsByRoute[route.id] ?: emptyList()
            val routeSegments = historySegments.filter { it.routeId == route.id }
            totalDistance += when {
                routePoints.isNotEmpty() ->
                    PrivacyZonePolicy.recordedDistanceMeters(routePoints, activeZones)
                hasHistoryExclusion -> 0.0
                else -> route.distanceMeters
            }
            totalDurationSeconds += when {
                routePoints.isNotEmpty() ->
                    PrivacyZonePolicy.recordedDurationSeconds(routePoints, activeZones)
                routeSegments.isNotEmpty() -> routeSegments.sumOf {
                    (it.endTime - it.startTime).coerceAtLeast(0L) / 1_000L
                }
                hasHistoryExclusion -> 0L
                else -> route.totalDurationSeconds
            }
        }

        for (segment in historySegments) {
            val duration = (segment.endTime - segment.startTime).coerceAtLeast(0L) / 1_000L
            when (segment.activityType) {
                ActivityType.WALKING, ActivityType.RUNNING -> walkingDuration += duration
                ActivityType.CYCLING, ActivityType.IN_VEHICLE, ActivityType.PUBLIC_TRANSPORT ->
                    transportDuration += duration
                ActivityType.STATIONARY -> stationaryDuration += duration
                ActivityType.UNKNOWN -> { }
            }
        }

        val segmentDurations = exposureSegments.associate { segment ->
            segment.id to (segment.endTime - segment.startTime).coerceAtLeast(0L) / 1_000L
        }
        val inputs = routeResults.mapNotNull { result ->
            if (result.stayId != null) return@mapNotNull null
            val durationSeconds = result.segmentId?.let(segmentDurations::get)
                ?: return@mapNotNull null
            DailyExposureInput(result, durationSeconds)
        }
        val totalTrackedSegmentSeconds = segmentDurations.values.sum()
        val metrics = calculateDailyExposureMetrics(inputs, totalTrackedSegmentSeconds)
        if (metrics == null) {
            dailyReportRepository.deleteByDate(date)
            return null
        }
        val dailyScore = metrics.normalizedScore
        val coverageScore = metrics.coveragePercent
        val confidenceScore = metrics.confidencePercent

        val segmentContributions = metrics.validInputs.map { input ->
            val result = input.result
            val confidence = result.confidence
            val durationHours = input.durationSeconds / 3_600.0
            val rawScore = result.rawExposureScore.coerceIn(0.0, RAW_SCORE_CAP)
            SegmentContribution(
                segmentId = result.segmentId ?: 0,
                stayId = result.stayId,
                rawScore = rawScore,
                normalizedScore = result.exposureScore,
                confidence = confidence,
                durationMinutes = kotlin.math.ceil(input.durationSeconds / 60.0)
                    .toInt()
                    .coerceAtLeast(1),
                contribution = rawScore * confidence * durationHours,
            )
        }

        val visitedPlaceIds = visibleStays.mapNotNull { it.placeId }.distinct()
        var visitedPlacesCount = visitedPlaceIds.size
        if (visitedPlacesCount == 0 && visibleStays.isNotEmpty()) {
            visitedPlacesCount = visibleStays.size
        }

        val orderedContributions = segmentContributions.sortedByDescending { it.contribution }
        val topContributors = orderedContributions.take(5)
        val topContributorsJson = buildTopContributorsJson(topContributors, visibleStays)

        val bestPeriod = orderedContributions.minByOrNull { it.normalizedScore }
        val worstPeriod = orderedContributions.maxByOrNull { it.normalizedScore }
        val bestPeriodJson = buildPeriodJson(bestPeriod, "best")
        val worstPeriodJson = buildPeriodJson(worstPeriod, "worst")

        val recommendations = generateRecommendations(
            dailyScore = dailyScore,
            coverage = coverageScore,
            confidence = confidenceScore,
            topContributors = topContributors,
            worstPeriod = worstPeriod,
        )
        val recommendationsJson = buildRecommendationsJson(recommendations)

        val report = DailyReport(
            date = date,
            totalScore = dailyScore,
            confidence = confidenceScore,
            coverage = coverageScore,
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = totalDurationSeconds,
            walkingDurationSeconds = walkingDuration,
            transportDurationSeconds = transportDuration,
            stationaryDurationSeconds = stationaryDuration,
            visitedPlacesCount = visitedPlacesCount,
            topContributorsJson = topContributorsJson,
            bestPeriodJson = bestPeriodJson,
            worstPeriodJson = worstPeriodJson,
            recommendationsJson = recommendationsJson,
            reportVersion = 3,
            generatedAt = System.currentTimeMillis(),
            isRegenerated = isRegenerated,
        )

        dailyReportRepository.insert(report)
        return report
    }

    fun normalizeDailyScore(rawScore: Double): Double =
        normalizeDailyExposureScore(rawScore)

    private data class SegmentContribution(
        val segmentId: Long,
        val stayId: Long?,
        val rawScore: Double,
        val normalizedScore: Double,
        val confidence: Double,
        val durationMinutes: Int,
        val contribution: Double,
    )

    private data class Recommendation(
        val type: String,
        val message: String,
    )

    private fun buildTopContributorsJson(
        contributors: List<SegmentContribution>,
        allStays: List<Stay>,
    ): String {
        if (contributors.isEmpty()) return "[]"
        val entries = contributors.map { c ->
            val label = c.stayId?.let { stayId ->
                allStays.find { it.id == stayId }?.let { stay ->
                    "Stay ${stay.startTime}"
                } ?: "Segment ${c.segmentId}"
            } ?: "Segment ${c.segmentId}"
            """{"label":"$label","contribution":${c.contribution.roundToLong()},"score":${c.normalizedScore.roundToLong()}}"""
        }
        return "[" + entries.joinToString(",") + "]"
    }

    private fun buildPeriodJson(contribution: SegmentContribution?, type: String): String? {
        if (contribution == null) return null
        return """{"type":"$type","score":${contribution.normalizedScore.roundToLong()},"durationMinutes":${contribution.durationMinutes}}"""
    }

    private fun generateRecommendations(
        dailyScore: Double,
        coverage: Double,
        confidence: Double,
        topContributors: List<SegmentContribution>,
        worstPeriod: SegmentContribution?,
    ): List<Recommendation> {
        val recs = mutableListOf<Recommendation>()

        if (dailyScore >= 70.0) {
            recs.add(Recommendation(
                "high_exposure",
                "Повышенный уровень экспозиции за день. Рассмотрите альтернативные маршруты или время поездок.",
            ))
        } else if (dailyScore >= 40.0) {
            recs.add(Recommendation(
                "moderate_exposure",
                "Умеренный уровень экспозиции. Отслеживайте периоды с наивысшими значениями.",
            ))
        } else {
            recs.add(Recommendation(
                "low_exposure",
                "Низкий уровень экспозиции за день. Текущие маршруты являются оптимальными с точки зрения нагрузки.",
            ))
        }

        if (coverage < 50.0) {
            recs.add(Recommendation(
                "low_coverage",
                "Недостаточно данных за день (${coverage.roundToLong()}% покрытия). Рекомендуется включить отслеживание на большее время.",
            ))
        }

        if (confidence < 50.0) {
            recs.add(Recommendation(
                "low_confidence",
                "Низкая достоверность данных (${confidence.roundToLong()}%). Проверьте датчики и качество локализации.",
            ))
        }

        worstPeriod?.let { wp ->
            recs.add(Recommendation(
                "peak_period",
                "Пиковая экспозиция зафиксирована в период длительностью ${wp.durationMinutes} мин (оценка ${wp.normalizedScore.toInt()}). Рассмотрите изменение времени прохождения этого участка.",
            ))
        }

        topContributors.firstOrNull()?.let { tc ->
            recs.add(Recommendation(
                "top_contributor",
                "Наибольший вклад в дневную оценку вносит сегмент \"${tc.label()}\". Рекомендуется проанализировать этот участок маршрута.",
            ))
        }

        return if (recs.isEmpty()) {
            listOf(Recommendation("general", "Данные за день обработаны. Отклонений не выявлено."))
        } else recs
    }

    private fun buildRecommendationsJson(recommendations: List<Recommendation>): String {
        if (recommendations.isEmpty()) return "[]"
        val entries = recommendations.map { r ->
            """{"type":"${r.type}","message":"${r.message.replace("\"", "\\\"")}"}"""
        }
        return "[" + entries.joinToString(",") + "]"
    }

    private fun SegmentContribution.label(): String {
        return "Segment $segmentId (${durationMinutes} мин, оценка ${normalizedScore.toInt()})"
    }

    override suspend fun regenerate(date: String): DailyReport? {
        return buildReport(date, isRegenerated = true)
    }
}

internal data class DailyExposureInput(
    val result: ExposureResult,
    val durationSeconds: Long,
)

internal data class DailyExposureMetrics(
    val normalizedScore: Double,
    val confidencePercent: Double,
    val coveragePercent: Double,
    val validInputs: List<DailyExposureInput>,
)

internal fun calculateDailyExposureMetrics(
    inputs: List<DailyExposureInput>,
    totalTrackedDurationSeconds: Long,
): DailyExposureMetrics? {
    val validInputs = inputs.filter { input ->
        val result = input.result
        input.durationSeconds > 0 &&
            result.rawExposureScore.isFinite() && result.rawExposureScore > 0.0 &&
            result.exposureScore.isFinite() && result.exposureScore in 0.0..100.0 &&
            result.pollution.isFinite() && result.pollution > 0.0 &&
            result.confidence.isFinite() && result.confidence in 0.0..1.0 &&
            result.confidence > 0.0 &&
            result.coverage.isFinite() && result.coverage in 0.0..100.0
    }
    if (validInputs.isEmpty() || totalTrackedDurationSeconds <= 0L) return null

    val totalDurationSeconds = validInputs.sumOf { it.durationSeconds.toDouble() }
    val validDurationSeconds = validInputs.sumOf { it.durationSeconds }
    val weightedRawScore = validInputs.sumOf { input ->
        input.result.rawExposureScore.coerceIn(0.0, DailyExposureAggregator.RAW_SCORE_CAP) *
            input.result.confidence *
            (input.durationSeconds / 3_600.0)
    }
    val confidencePercent = validInputs.sumOf { input ->
        input.result.confidence * input.durationSeconds.toDouble()
    } / totalDurationSeconds * 100.0
    val coveragePercent = (validDurationSeconds.toDouble() /
        totalTrackedDurationSeconds * 100.0).coerceIn(0.0, 100.0)

    return DailyExposureMetrics(
        normalizedScore = normalizeDailyExposureScore(weightedRawScore),
        confidencePercent = confidencePercent,
        coveragePercent = coveragePercent,
        validInputs = validInputs,
    )
}

internal fun normalizeDailyExposureScore(rawScore: Double): Double {
    val clamped = rawScore.coerceAtLeast(0.0)
    val normalized = 100.0 * (1.0 - Math.exp(-clamped / 50.0))
    return normalized.coerceIn(0.0, 100.0)
}
