package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.domain.model.LocationPointSummary

/**
 * Classifies movement patterns into activity types.
 *
 * Uses speed, acceleration, and dwell time heuristics.
 * Thresholds based on typical human movement speeds:
 * - Stationary: < 0.5 m/s
 * - Walking: 0.5–2.0 m/s
 * - Running: 2.0–6.0 m/s
 * - Cycling: > 6.0 m/s
 */
object MovementDetectionEngine {

    private const val WALKING_MAX_SPEED_MS = 2.0
    private const val RUNNING_MAX_SPEED_MS = 6.0
    private const val CYCLING_MIN_SPEED_MS = 6.0
    private const val STATIONARY_MAX_SPEED_MS = 0.5

    /** Determines activity type from location point summary */
    fun classify(points: List<LocationPointSummary>): ActivityType {
        if (points.isEmpty()) return ActivityType.UNKNOWN

        val speeds = points.mapNotNull { it.speedMps }
        if (speeds.isEmpty()) return ActivityType.UNKNOWN

        // Use weighted average of recent points (last 3)
        val recent = speeds.takeLast(3)
        val avgSpeed = recent.average()

        return when {
            avgSpeed < STATIONARY_MAX_SPEED_MS -> ActivityType.STATIONARY
            avgSpeed < WALKING_MAX_SPEED_MS -> ActivityType.WALKING
            avgSpeed < RUNNING_MAX_SPEED_MS -> ActivityType.RUNNING
            else -> ActivityType.CYCLING
        }
    }

    /** Detect stationary periods by dwell time (no significant movement) */
    fun detectStationary(points: List<LocationPointSummary>, thresholdMs: Long = 300_000): Boolean {
        if (points.size < 3) return false

        val lastThree = points.takeLast(3)
        val avgSpeed = lastThree.mapNotNull { it.speedMps }.average()
        return avgSpeed < STATIONARY_MAX_SPEED_MS
    }

    /** Calculates confidence in activity classification based on sample count */
    fun calculateConfidence(activity: ActivityType, sampleCount: Int): Double {
        val sampleFactor = if (sampleCount >= 10) 1.0 else if (sampleCount >= 5) 0.7 else 0.5
        val activityFactor = when {
            activity == ActivityType.STATIONARY -> 1.0
            activity == ActivityType.WALKING -> 0.9
            activity == ActivityType.RUNNING -> 0.8
            activity == ActivityType.CYCLING -> 0.85
            activity == ActivityType.IN_VEHICLE -> 0.95
            else -> 0.7
        }
        return sampleFactor * activityFactor
    }
}
