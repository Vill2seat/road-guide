package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.domain.model.LocationPointSummary
import java.time.Instant
import java.time.ZoneId

object RouteSegmentationEngine {

    fun segmentPoints(
        routeId: Long,
        routeStartTime: Long,
        points: List<LocationPointSummary>,
        routeDistance: Double
    ): List<RouteSegment> {
        if (points.isEmpty()) return emptyList()
        if (points.size < 2) {
            return listOf(stayFromPoint(routeId, points.first(), routeStartTime))
        }
        val segments = mutableListOf<RouteSegment>()
        var current: RouteSegment? = null
        var currentActivity: ActivityType? = null
        for (i in points.indices) {
            val point = points[i]
            val activity = determineActivityType(point)
            if (current == null) {
                current = makeSegment(routeId, routeStartTime, point, activity, i)
                currentActivity = activity
            } else if (activity != currentActivity) {
                closeSegment(current, points, i)
                current = makeSegment(routeId, routeStartTime, point, activity, i)
                currentActivity = activity
            } else {
                extendSegment(current, point)
            }
        }
        current?.let { closeSegment(it, points, points.size) }
        return segments.filter { it.endTime > it.startTime }
    }

    private fun determineActivityType(point: LocationPointSummary): ActivityType {
        if (point.activityType == "stationary" || point.speedMps < 0.5) {
            return ActivityType.STATIONARY
        }
        return when {
            point.speedMps < 2.0 -> ActivityType.WALKING
            point.speedMps < 6.0 -> ActivityType.RUNNING
            else -> ActivityType.CYCLING
        }
    }

    private fun makeSegment(
        routeId: Long,
        routeStartTime: Long,
        points: List<LocationPointSummary>,
        activity: ActivityType?,
        pointIndex: Int
    ): RouteSegment {
        val point = points[pointIndex]
        val startTime = point.timestamp
        val endTime = if (pointIndex < points.size - 1) {
            points[pointIndex + 1].timestamp
        } else {
            System.currentTimeMillis()
        }
        return RouteSegment(
            routeId = routeId, startTime = startTime, endTime = endTime,
            activityType = activity ?: ActivityType.UNKNOWN, distanceMeters = 0.0,
            startLatitude = point.latitude, startLongitude = point.longitude,
            endLatitude = point.latitude, endLongitude = point.longitude, confidence = 0.5
        )
    }

    private fun closeSegment(segment: RouteSegment, points: List<LocationPointSummary>, endIndex: Int) {
        if (endIndex <= 0) return
        val startIndex = findStart(segment, points)
        if (startIndex < 0) return
        val sub = points.subList(startIndex, endIndex + 1)
        val duration = (segment.endTime - segment.startTime) / 1000
        val distance = calcDistance(sub)
        if (duration < 60 || distance < 50.0) return
        segment.distanceMeters = distance
    }

    private fun findStart(segment: RouteSegment, points: List<LocationPointSummary>): Int {
        for (i in points.indices) {
            if (points[i].timestamp >= segment.startTime) return i
        }
        return -1
    }

    private fun calcDistance(points: List<LocationPointSummary>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += haversine(points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude)
        }
        return total
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private fun stayFromPoint(routeId: Long, point: LocationPointSummary, routeStartTime: Long): RouteSegment {
        return RouteSegment(routeId = routeId, startTime = point.timestamp,
            endTime = System.currentTimeMillis(), activityType = ActivityType.STATIONARY,
            distanceMeters = 0.0, startLatitude = point.latitude, startLongitude = point.longitude,
            endLatitude = point.latitude, endLongitude = point.longitude, confidence = 0.8)
    }

    private fun extendSegment(segment: RouteSegment, point: LocationPointSummary) {
        // Extend segment end time to include this point
        if (point.timestamp > segment.endTime) {
            segment.endTime = point.timestamp
        }
    }

    fun finalizeRoute(
        routeId: Long,
        routeStartTime: Long,
        points: List<LocationPointSummary>,
        routeType: RouteType = RouteType.DAILY_AUTO,
        routeDistance: Double = 0.0
    ): Route {
        val totalDuration = if (points.isNotEmpty()) {
            points.last().timestamp - routeStartTime
        } else {
            3600_000L
        }
        val status = if (points.isNotEmpty()) RouteStatus.COMPLETED else RouteStatus.IN_PROGRESS
        return Route(
            id = routeId,
            startTime = routeStartTime,
            endTime = if (status == RouteStatus.COMPLETED) System.currentTimeMillis() else null,
            distanceMeters = routeDistance,
            status = status,
            routeType = routeType,
            confidence = 0.7,
            date = Instant.ofEpochMilli(routeStartTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString(),
            totalDurationSeconds = totalDuration / 1000,
            calculationVersion = 1
        )
    }
}
