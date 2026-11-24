package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.domain.model.LocationPointSummary
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MovementDetectionEngine {

    private const val WALKING_MAX_SPEED_MS = 2.0
    private const val RUNNING_MAX_SPEED_MS = 6.0
    private const val CYCLING_MAX_SPEED_MS = 10.0
    private const val STATIONARY_MAX_SPEED_MS = 0.5

    private const val IN_VEHICLE_MAX_SPEED_MS = 30.0
    private const val IN_VEHICLE_ACCEL_THRESHOLD_MS2 = 3.0

    private const val PT_MIN_STOPS_IN_WINDOW = 2
    private const val PT_STOP_SPEED_MS = 0.5
    private const val PT_STOP_DURATION_MS = 30_000
    private const val STAY_MAX_RADIUS_METERS = 50.0
    private const val MAX_CONTIGUOUS_GAP_MS = 5 * 60 * 1000L

    data class ClassificationResult(
        val activity: ActivityType,
        val confidence: Double,
        val factors: Map<String, Double>
    )

    private fun calculateAcceleration(prev: LocationPointSummary, curr: LocationPointSummary): Double {
        val deltaV = abs(curr.speedMps - prev.speedMps)
        val deltaT = (curr.timestamp - prev.timestamp) / 1000.0
        return if (deltaT > 0) deltaV / deltaT else 0.0
    }

    private fun dAbs(d: Double) = abs(d)

    private fun ptStopCountScore(points: List<LocationPointSummary>): Double {
        if (points.size < 10) return 0.0
        var stops = 0
        var i = 0
        while (i < points.size) {
            val moving = points[i].speedMps > PT_STOP_SPEED_MS
            var j = i
            while (j < points.size && (points[j].speedMps > PT_STOP_SPEED_MS) == moving) j++
            if (j > i) {
                val segDur = points[j - 1].timestamp - points[i].timestamp
                if (!moving && segDur >= PT_STOP_DURATION_MS) stops++
            }
            i = j
        }
        return if (stops >= PT_MIN_STOPS_IN_WINDOW) 1.0 else 0.0
    }

    private fun normalizeScore(v: Double, minV: Double, maxV: Double): Double =
        if (maxV > minV) ((v - minV) / (maxV - minV + 0.001)).coerceIn(0.0, 1.0)
        else 0.5

    private fun classifyWithResult(points: List<LocationPointSummary>): ClassificationResult {
        if (points.isEmpty()) return ClassificationResult(ActivityType.UNKNOWN, 0.0, emptyMap())

        val n = points.size
        val avgSpeed: Double = if (points.isNotEmpty()) {
            var sum = 0.0
            for (p in points) sum += p.speedMps
            sum / points.size
        } else 0.0

        var accScore = 0.5
        if (n >= 2) {
            val accVals = (1 until n).map { i -> calculateAcceleration(points[i - 1], points[i]) }
            val avgAcc = accVals.average()
            val absAvg = dAbs(avgAcc)
            accScore = if (absAvg > IN_VEHICLE_ACCEL_THRESHOLD_MS2) 0.2
                else if (absAvg > 1.0) 0.5
                else 0.8
        }

        val ptScore = ptStopCountScore(points)

        val activity = when {
            ptScore > 0.0 -> ActivityType.PUBLIC_TRANSPORT
            avgSpeed < STATIONARY_MAX_SPEED_MS -> ActivityType.STATIONARY
            avgSpeed < WALKING_MAX_SPEED_MS -> ActivityType.WALKING
            avgSpeed < RUNNING_MAX_SPEED_MS -> ActivityType.RUNNING
            avgSpeed < CYCLING_MAX_SPEED_MS -> ActivityType.CYCLING
            avgSpeed <= IN_VEHICLE_MAX_SPEED_MS && accScore >= 0.5 -> ActivityType.IN_VEHICLE
            avgSpeed > IN_VEHICLE_MAX_SPEED_MS -> ActivityType.IN_VEHICLE
            else -> ActivityType.CYCLING
        }

        val confidence = when (activity) {
            ActivityType.STATIONARY -> if (avgSpeed < 0.2) 0.9 else 0.7
            ActivityType.WALKING -> 0.8
            ActivityType.RUNNING -> 0.75
            ActivityType.CYCLING -> if (accScore <= 0.2) 0.6 else 0.8
            ActivityType.IN_VEHICLE -> if (accScore >= 0.8) 0.9 else 0.7
            ActivityType.PUBLIC_TRANSPORT -> if (ptScore > 0) 0.85 else 0.5
            else -> 0.0
        }

        val factors = mutableMapOf<String, Double>()
        factors["speed"] = normalizeScore(avgSpeed, 0.0, 20.0)
        factors["acceleration"] = normalizeScore(accScore, 0.0, 1.0)
        factors["pt_pattern"] = ptScore

        return ClassificationResult(activity, confidence, factors.toMap())
    }

    fun classify(points: List<LocationPointSummary>): ActivityType =
        classifyWithResult(points).activity

    fun detectStationary(points: List<LocationPointSummary>, thresholdMs: Long = 300_000): Boolean {
        if (points.size < 2) return false
        val ordered = points.sortedBy { it.timestamp }
        if (ordered.zipWithNext().any { (previous, current) ->
                current.timestamp - previous.timestamp > MAX_CONTIGUOUS_GAP_MS
            }
        ) {
            return false
        }
        if (ordered.last().timestamp - ordered.first().timestamp < thresholdMs) return false
        if (ordered.any { it.speedMps >= STATIONARY_MAX_SPEED_MS }) return false

        val centerLatitude = ordered.map { it.latitude }.average()
        val centerLongitude = ordered.map { it.longitude }.average()
        return ordered.all { point ->
            distanceMeters(
                point.latitude,
                point.longitude,
                centerLatitude,
                centerLongitude
            ) <= STAY_MAX_RADIUS_METERS
        }
    }

    private fun distanceMeters(
        firstLatitude: Double,
        firstLongitude: Double,
        secondLatitude: Double,
        secondLongitude: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val latitudeDelta = Math.toRadians(secondLatitude - firstLatitude)
        val longitudeDelta = Math.toRadians(secondLongitude - firstLongitude)
        val firstLatitudeRadians = Math.toRadians(firstLatitude)
        val secondLatitudeRadians = Math.toRadians(secondLatitude)
        val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(firstLatitudeRadians) * cos(secondLatitudeRadians) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        return earthRadiusMeters * 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
    }

    fun calculateConfidence(activity: ActivityType, sampleCount: Int): Double {
        val sf = if (sampleCount >= 10) 1.0 else if (sampleCount >= 5) 0.7 else 0.5
        val af = when (activity) {
            ActivityType.STATIONARY -> 1.0
            ActivityType.WALKING -> 0.9
            ActivityType.RUNNING -> 0.8
            ActivityType.CYCLING -> 0.85
            ActivityType.IN_VEHICLE -> 0.95
            else -> 0.7
        }
        return sf * af
    }
}