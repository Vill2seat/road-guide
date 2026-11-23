package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.local.entity.DailyReport
import com.roadguide.app.data.local.entity.ExposureResult
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.data.local.entity.Stay
import com.roadguide.app.data.repository.DailyReportRepository
import com.roadguide.app.data.repository.ExposureResultRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.repository.RouteSegmentRepository
import com.roadguide.app.data.repository.StayRepository
import com.roadguide.app.domain.usecase.AggregateDailyExposure
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

class DailyExposureAggregator(
    private val routeRepository: RouteRepository,
    private val routeSegmentRepository: RouteSegmentRepository,
    private val exposureResultRepository: ExposureResultRepository,
    private val stayRepository: StayRepository,
    private val dailyReportRepository: DailyReportRepository,
) : AggregateDailyExposure {

    companion object {
        private const val SECONDS_PER_DAY: Long = 86_400L
        private const val RAW_SCORE_CAP: Double = 200.0
    }

    override suspend fun aggregate(date: String): DailyReport? {
        return buildReport(date, isRegenerated = false)
    }
    private suspend fun buildReport(date: String, isRegenerated: Boolean): DailyReport? {
        val routes = routeRepository.getByDate(date).first() ?: return null
        if (routes.isEmpty()) return null

        val routeIds = routes.map { it.id }.toSet()

        val allResults = exposureResultRepository.observeAll().first()
        val routeResults = allResults.filter { it.routeId in routeIds }
        if (routeResults.isEmpty()) return null

        val allSegments = routeIds.flatMap { routeId ->
            routeSegmentRepository.observeByRoute(routeId).first()
        }

        val allStays = routeIds.flatMap { routeId ->
            stayRepository.observeByRoute(routeId).first()
        }

        var walkingDuration = 0L
        var transportDuration = 0L
        var stationaryDuration = 0L
        var totalDistance = 0.0
        var totalDurationSeconds = 0L

        for (route in routes) {
            totalDistance += route.distanceMeters
            totalDurationSeconds += route.totalDurationSeconds
        }

        for (segment in allSegments) {
            val duration = segment.endTime - segment.startTime
            when (segment.activityType) {
                ActivityType.WALKING, ActivityType.RUNNING -> walkingDuration += duration
                ActivityType.CYCLING, ActivityType.IN_VEHICLE, ActivityType.PUBLIC_TRANSPORT ->
                    transportDuration += duration
                ActivityType.STATIONARY -> stationaryDuration += duration
                ActivityType.UNKNOWN -> { }
            }
        }

        var dailyRawScore = 0.0
        var totalConfidenceSum = 0.0
        var validSegmentCount = 0

        val segmentContributions = mutableListOf<SegmentContribution>()

        for (result in routeResults) {
            val rawScore = result.rawExposureScore.coerceIn(0.0, RAW_SCORE_CAP)
            val confidence = result.confidence.coerceIn(0.0, 1.0)
            val durationHours = result.durationMinutes / 60.0
            val weight = confidence * durationHours

            dailyRawScore += rawScore * weight
            totalConfidenceSum += confidence
            validSegmentCount++

            segmentContributions.add(
                SegmentContribution(
                    segmentId = result.segmentId ?: 0,
                    stayId = result.stayId,
                    rawScore = rawScore,
                    normalizedScore = result.exposureScore,
                    confidence = confidence,
                    durationMinutes = result.durationMinutes,
                    contribution = rawScore * weight,
                )
            )
        }

        val dailyScore = normalizeDailyScore(dailyRawScore)

        val coverageScore = if (totalDurationSeconds > 0) {
            (totalDurationSeconds.toDouble() / SECONDS_PER_DAY) * 100.0
        } else 0.0

        val confidenceScore = if (validSegmentCount > 0) {
            (totalConfidenceSum / validSegmentCount) * 100.0
        } else 0.0

        val visitedPlaceIds = allStays.mapNotNull { it.placeId }.distinct()
        var visitedPlacesCount = visitedPlaceIds.size
        if (visitedPlacesCount == 0 && allStays.isNotEmpty()) {
            visitedPlacesCount = allStays.size
        }

        segmentContributions.sortByDescending { it.contribution }
        val topContributors = segmentContributions.take(5)
        val topContributorsJson = buildTopContributorsJson(topContributors, allStays)

        val bestPeriod = segmentContributions.minByOrNull { it.normalizedScore }
        val worstPeriod = segmentContributions.maxByOrNull { it.normalizedScore }
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
            reportVersion = 2,
            generatedAt = System.currentTimeMillis(),
            isRegenerated = isRegenerated,
        )

        dailyReportRepository.insert(report)
        return report
    }

    fun normalizeDailyScore(rawScore: Double): Double {
        val clamped = rawScore.coerceAtLeast(0.0)
        val scale = 50.0
        val normalized = 100.0 * (1.0 - Math.exp(-clamped / scale))
        return normalized.coerceIn(0.0, 100.0)
    }

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
                "Недостаточно данных за день ($coverage.toInt% покрытия). Рекомендуется включить отслеживание на большее время.",
            ))
        }

        if (confidence < 50.0) {
            recs.add(Recommendation(
                "low_confidence",
                "Низкая достоверность данных ($confidence.toInt%). Проверьте датчики и качество локализации.",
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
