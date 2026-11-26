package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.local.entity.PrivacyZone
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.data.local.entity.Stay
import com.roadguide.app.domain.model.LocationPointSummary
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RouteSegmentationEngine {

    const val MAX_GPS_GAP_MS = PrivacyZonePolicy.MAX_TRACKED_GAP_MS
    const val MIN_ACTIVITY_SEGMENT_DURATION_MS = 30_000L
    const val MIN_STAY_DURATION_MS = 5 * 60 * 1000L
    private const val PUBLIC_TRANSPORT_WINDOW_SIZE = 12

    data class Analysis(
        val segments: List<RouteSegment>,
        val stays: List<Stay>
    )

    fun segmentPoints(
        routeId: Long,
        routeStartTime: Long,
        points: List<LocationPointSummary>,
        @Suppress("UNUSED_PARAMETER") routeDistance: Double,
        privacyZones: List<PrivacyZone> = emptyList()
    ): List<RouteSegment> =
        analyzePoints(routeId, routeStartTime, points, privacyZones).segments

    fun analyzePoints(
        routeId: Long,
        routeStartTime: Long,
        points: List<LocationPointSummary>,
        privacyZones: List<PrivacyZone> = emptyList()
    ): Analysis {
        val orderedPoints = points
            .asSequence()
            .filter { it.timestamp >= routeStartTime }
            .filterNot { point ->
                PrivacyZonePolicy.decisionAt(
                    point.latitude,
                    point.longitude,
                    privacyZones
                ).excludeFromHistory
            }
            .sortedBy { it.timestamp }
            .distinctBy { it.timestamp }
            .toList()

        if (orderedPoints.size < 2) return Analysis(emptyList(), emptyList())

        val segments = mutableListOf<RouteSegment>()
        val stays = mutableListOf<Stay>()
        var groupStart = 0
        var groupActivity = activityAt(orderedPoints, 0, privacyZones)
        var pendingActivity: ActivityType? = null
        var pendingStart = -1
        var pendingCount = 0

        for (index in 1 until orderedPoints.size) {
            val previous = orderedPoints[index - 1]
            val current = orderedPoints[index]
            val gap = current.timestamp - previous.timestamp
            val crossesPrivateZone = PrivacyZonePolicy.excludesPath(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude,
                privacyZones,
                PrivacyScope.HISTORY
            )

            if (gap > MAX_GPS_GAP_MS || crossesPrivateZone) {
                addGroup(
                    routeId = routeId,
                    points = orderedPoints,
                    startIndex = groupStart,
                    endIndex = index - 1,
                    activity = groupActivity,
                    segments = segments,
                    stays = stays,
                    privacyZones = privacyZones,
                    routeStartTime = routeStartTime
                )
                groupStart = index
                groupActivity = activityAt(orderedPoints, index, privacyZones)
                pendingActivity = null
                pendingStart = -1
                pendingCount = 0
                continue
            }

            val currentActivity = activityAt(orderedPoints, index, privacyZones)
            if (currentActivity == groupActivity) {
                pendingActivity = null
                pendingStart = -1
                pendingCount = 0
            } else {
                if (currentActivity != pendingActivity) {
                    pendingActivity = currentActivity
                    pendingStart = index
                    pendingCount = 1
                } else {
                    pendingCount++
                }

                if (pendingCount >= 2) {
                    addGroup(
                        routeId = routeId,
                        points = orderedPoints,
                        startIndex = groupStart,
                        endIndex = pendingStart - 1,
                        activity = groupActivity,
                        segments = segments,
                        stays = stays,
                        privacyZones = privacyZones,
                        routeStartTime = routeStartTime
                    )
                    groupStart = pendingStart
                    groupActivity = currentActivity
                    pendingActivity = null
                    pendingStart = -1
                    pendingCount = 0
                }
            }
        }

        addGroup(
            routeId = routeId,
            points = orderedPoints,
            startIndex = groupStart,
            endIndex = orderedPoints.lastIndex,
            activity = groupActivity,
            segments = segments,
            stays = stays,
            privacyZones = privacyZones,
            routeStartTime = routeStartTime
        )

        return Analysis(segments, stays)
    }

    fun recordedDistanceMeters(
        points: List<LocationPointSummary>,
        privacyZones: List<PrivacyZone> = emptyList()
    ): Double {
        val orderedPoints = points
            .filterNot { point ->
                PrivacyZonePolicy.decisionAt(
                    point.latitude,
                    point.longitude,
                    privacyZones
                ).excludeFromHistory
            }
            .sortedBy { it.timestamp }
            .distinctBy { it.timestamp }
        return orderedPoints.zipWithNext().sumOf { (previous, current) ->
            if (
                current.timestamp - previous.timestamp > MAX_GPS_GAP_MS ||
                PrivacyZonePolicy.excludesPath(
                    previous.latitude,
                    previous.longitude,
                    current.latitude,
                    current.longitude,
                    privacyZones,
                    PrivacyScope.HISTORY
                )
            ) 0.0
            else distanceMeters(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
        }
    }

    private fun activityAt(
        points: List<LocationPointSummary>,
        index: Int,
        privacyZones: List<PrivacyZone>
    ): ActivityType {
        val point = points[index]
        var contiguousStart = index
        while (contiguousStart > 0 &&
            points[contiguousStart].timestamp - points[contiguousStart - 1].timestamp <= MAX_GPS_GAP_MS &&
            !PrivacyZonePolicy.excludesPath(
                points[contiguousStart - 1].latitude,
                points[contiguousStart - 1].longitude,
                points[contiguousStart].latitude,
                points[contiguousStart].longitude,
                privacyZones,
                PrivacyScope.HISTORY
            )
        ) {
            contiguousStart--
        }
        val windowStart = maxOf(contiguousStart, index - PUBLIC_TRANSPORT_WINDOW_SIZE + 1)
        val transportWindow = points.subList(windowStart, index + 1)
        if (transportWindow.size >= 10 &&
            MovementDetectionEngine.classify(transportWindow) == ActivityType.PUBLIC_TRANSPORT
        ) {
            return ActivityType.PUBLIC_TRANSPORT
        }

        return point.activityType.toActivityType()
            ?: MovementDetectionEngine.classify(listOf(point))
    }

    private fun String.toActivityType(): ActivityType? = when (lowercase()) {
        "stationary" -> ActivityType.STATIONARY
        "walking" -> ActivityType.WALKING
        "running" -> ActivityType.RUNNING
        "cycling" -> ActivityType.CYCLING
        "in_vehicle", "in vehicle" -> ActivityType.IN_VEHICLE
        "public_transport", "public transport" -> ActivityType.PUBLIC_TRANSPORT
        else -> null
    }

    private fun addGroup(
        routeId: Long,
        points: List<LocationPointSummary>,
        startIndex: Int,
        endIndex: Int,
        activity: ActivityType,
        segments: MutableList<RouteSegment>,
        stays: MutableList<Stay>,
        privacyZones: List<PrivacyZone>,
        routeStartTime: Long
    ) {
        if (endIndex <= startIndex) return

        val group = points.subList(startIndex, endIndex + 1)
        val first = group.firstOrNull() ?: return
        val last = group.last()
        val duration = last.timestamp - first.timestamp
        if (duration < MIN_ACTIVITY_SEGMENT_DURATION_MS) return

        val distance = recordedDistanceMeters(group, privacyZones)
        segments += RouteSegment(
            routeId = routeId,
            startTime = first.timestamp,
            endTime = last.timestamp,
            activityType = activity,
            distanceMeters = distance,
            startLatitude = first.latitude,
            startLongitude = first.longitude,
            endLatitude = last.latitude,
            endLongitude = last.longitude,
            confidence = MovementDetectionEngine.calculateConfidence(activity, group.size)
        )

        if (activity == ActivityType.STATIONARY &&
            MovementDetectionEngine.detectStationary(group, MIN_STAY_DURATION_MS)
        ) {
            stays += makeStay(routeId, group, routeStartTime)
        }
    }

    private fun makeStay(routeId: Long, points: List<LocationPointSummary>, routeStartTime: Long): Stay {
        val centerLatitude = points.map { it.latitude }.average()
        val centerLongitude = points.map { it.longitude }.average()
        val radius = points.maxOf { point ->
            distanceMeters(
                point.latitude,
                point.longitude,
                centerLatitude,
                centerLongitude
            )
        }
        return Stay(
            routeId = routeId,
            startTime = points.firstOrNull()?.timestamp ?: routeStartTime,
            endTime = points.lastOrNull()?.timestamp ?: routeStartTime,
            centerLatitude = centerLatitude,
            centerLongitude = centerLongitude,
            radiusMeters = radius,
            confidence = MovementDetectionEngine.calculateConfidence(
                ActivityType.STATIONARY,
                points.size
            )
        )
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
}
