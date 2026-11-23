package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.domain.model.ExposureConstants
import com.roadguide.app.domain.model.RouteEvaluation
import com.roadguide.app.domain.model.RouteCategory
import com.roadguide.app.domain.model.RouteProfile
import com.roadguide.app.map.model.MapPoint
import com.roadguide.app.data.repository.EnvironmentalSampleRepository
import kotlin.math.roundToInt

class RouteEvaluationEngine(
    private val environmentalSampleRepository: EnvironmentalSampleRepository,
    private val geographicContextEngine: GeographicContextEngine
) {

    companion object {
        private const val SEGMENT_STEP_POINTS = 5
    }

    suspend fun evaluateRoute(
        id: String,
        geometry: List<MapPoint>,
        durationSeconds: Long,
        category: RouteCategory,
        profile: RouteProfile
    ): RouteEvaluation {
        if (geometry.isEmpty()) {
            return RouteEvaluation(
                id = id,
                distanceMeters = 0.0,
                durationSeconds = durationSeconds,
                geometry = emptyList(),
                exposureScore = 50.0,
                confidence = 0.0,
                coverage = 0.0,
                pollutionContext = null,
                roadProximity = 0.0,
                greenAreaProximity = 0.0,
                category = category,
                summary = "Нет данных для оценки"
            )
        }

        var distanceMeters = 0.0
        for (i in 1 until geometry.size) {
            distanceMeters += calculateDistanceMeters(
                geometry[i - 1].latitude, geometry[i - 1].longitude,
                geometry[i].latitude, geometry[i].longitude
            )
        }

        val segmentIndices = geometry.indices.filter { it % SEGMENT_STEP_POINTS == 0 }
        val segmentPoints = segmentIndices.map { geometry[it] }

        var totalRawExposure = 0.0
        var totalConfidenceSum = 0.0
        var validSegments = 0
        var pointsWithEnv = 0
        var greenCount = 0
        var roadCount = 0

        for (point in segmentPoints) {
            val roadFactor = geographicContextEngine.getRoadFactor(point.latitude, point.longitude)

            val envSample = environmentalSampleRepository.fetchAndSave(point.latitude, point.longitude)
            if (envSample != null) {
                pointsWithEnv++
            }

            val segment = com.roadguide.app.data.local.entity.RouteSegment(
                routeId = 0,
                startTime = 0,
                endTime = 60,
                activityType = when (profile) {
                    RouteProfile.WALKING -> com.roadguide.app.data.local.entity.ActivityType.WALKING
                    RouteProfile.CYCLING -> com.roadguide.app.data.local.entity.ActivityType.CYCLING
                    RouteProfile.DRIVING -> com.roadguide.app.data.local.entity.ActivityType.IN_VEHICLE
                },
                distanceMeters = if (geometry.size > 1) distanceMeters / geometry.size.coerceAtLeast(2).toDouble() else 0.0,
                startLatitude = point.latitude,
                startLongitude = point.longitude,
                endLatitude = point.latitude,
                endLongitude = point.longitude,
                confidence = com.roadguide.app.domain.usecase.impl.MovementDetectionEngine.calculateConfidence(
                    com.roadguide.app.data.local.entity.ActivityType.WALKING, 5
                )
            )

            val rawExposure = com.roadguide.app.domain.usecase.impl.ExposureCalculationEngine.calculateSegmentExposure(
                segment = segment,
                sample = envSample,
                roadFactor = roadFactor
            )

            totalRawExposure += rawExposure
            validSegments++

            val context = geographicContextEngine.getContext(point.latitude, point.longitude)
            if (context.isGreenArea) greenCount++
            if (context.isMajorRoad || context.roadType != null) roadCount++

            totalConfidenceSum += segment.confidence
        }

        val avgRawExposure = if (validSegments > 0) totalRawExposure / validSegments else 0.0
        val exposureScore = com.roadguide.app.domain.usecase.impl.ExposureCalculationEngine.normalizeExposure(avgRawExposure)

        val confidence = if (validSegments > 0) totalConfidenceSum / validSegments else 0.0
        val coverage = if (segmentPoints.isNotEmpty()) (pointsWithEnv.toDouble() / segmentPoints.size) * 100.0 else 0.0

        val pollutionContext = when {
            greenCount > segmentPoints.size / 2 -> "green_area"
            roadCount > segmentPoints.size / 2 -> "major_road"
            else -> "mixed"
        }

        val roadProximity = if (segmentPoints.isNotEmpty()) roadCount.toDouble() / segmentPoints.size else 0.0
        val greenAreaProximity = if (segmentPoints.isNotEmpty()) greenCount.toDouble() / segmentPoints.size else 0.0

        val summary = buildSummary(exposureScore, confidence, coverage, distanceMeters, durationSeconds)

        return RouteEvaluation(
            id = id,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            geometry = geometry,
            exposureScore = exposureScore,
            confidence = confidence,
            coverage = coverage,
            pollutionContext = pollutionContext,
            roadProximity = roadProximity,
            greenAreaProximity = greenAreaProximity,
            category = category,
            summary = summary
        )
    }

    private fun buildSummary(
        exposureScore: Double,
        confidence: Double,
        coverage: Double,
        distanceMeters: Double,
        durationSeconds: Long
    ): String {
        val distKm = (distanceMeters / 1000.0).roundToInt()
        val durMin = (durationSeconds / 60.0).roundToInt()
        val exposureLabel = when {
            exposureScore < 30 -> "Отличная экология"
            exposureScore < 60 -> "Умеренная нагрузка"
            else -> "Повышенная нагрузка"
        }
        return "~$distKm км, ~$durMin мин, $exposureLabel (уверенность: ${(confidence * 100).roundToInt()}%)"
    }

    private fun calculateDistanceMeters(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}