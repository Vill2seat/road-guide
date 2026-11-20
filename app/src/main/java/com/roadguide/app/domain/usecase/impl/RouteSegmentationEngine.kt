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
            // Single point - treat as potential stay
            return stayFromPoint(routeId, listOf(points.first()), routeStartTime)
        }
        val segments = mutableListOf<RouteSegment>()
        var current: RouteSegment? = null
        var currentActivity: ActivityType? = null
        for (i in points.indices) {
            val point = points[i]
            val activity = determineActivityType(point)
            if (current == null) {
                current = makeSegment(routeId, routeStartTime, points, activity, i)
                segments.add(current)
                currentActivity = activity
            } else if (activity != currentActivity) {
                closeSegment(current, points, i)
                segments.add(current)
                current = makeSegment(routeId, routeStartTime, points, activity, i)
                segments.add(current)
                currentActivity = activity
            } else {
                extendSegment(current, point)
            }
        }
        current?.let { closeSegment(it, points, points.size) }
        segments.add(current!!)
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

    private fun stayFromPoint(routeId: Long, points: List<LocationPointSummary>, routeStartTime: Long): List<RouteSegment> {
        // Use MovementDetectionEngine.detectStationary to find stationary periods
        // Default threshold is 300_000 ms (5 minutes) of speed < 0.5 m/s
        val segments = mutableListOf<RouteSegment>()

        // Need at least 3 points for detectStationary to work
        if (points.size < 3) {
            // Single or double point - treat as potential stay if speed is very low
            val slowPoints = points.filter { it.speedMps < 0.5 }
            if (slowPoints.isNotEmpty()) {
                return listOf(RouteSegment(
                    routeId = routeId,
                    startTime = slowPoints.first().timestamp,
                    endTime = if (slowPoints.size > 1) slowPoints.last().timestamp else System.currentTimeMillis(),
                    activityType = ActivityType.STATIONARY,
                    distanceMeters = 0.0,
                    startLatitude = slowPoints.first().latitude,
                    startLongitude = slowPoints.first().longitude,
                    endLatitude = if (slowPoints.size > 1) slowPoints.last().latitude else slowPoints.first().latitude,
                    endLongitude = if (slowPoints.size > 1) slowPoints.last().longitude else slowPoints.first().longitude,
                    confidence = 0.8
                ))
            }
            return emptyList()
        }

        // Check sliding windows of 3 points for stationary periods
        var i = 0
        while (i < points.size) {
            if (i < 2) { i++; continue }
            val window = points.slice(i - 2..i)
            if (MovementDetectionEngine.detectStationary(window)) {
                // This point is part of a stay - find the start and end of the stay
                val stayStart = findStayStart(points, i)
                val stayEnd = findStayEnd(points, i)
                if (stayStart <= stayEnd && stayStart >= 0 && stayEnd < points.size) {
                    val startPoint = points[stayStart]
                    val endPoint = points[stayEnd]
                    segments.add(RouteSegment(
                        routeId = routeId,
                        startTime = startPoint.timestamp,
                        endTime = endPoint.timestamp,
                        activityType = ActivityType.STATIONARY,
                        distanceMeters = 0.0,
                        startLatitude = startPoint.latitude,
                        startLongitude = startPoint.longitude,
                        endLatitude = endPoint.latitude,
                        endLongitude = endPoint.longitude,
                        confidence = 0.8
                    ))
                    // Skip past this stay to avoid duplicates
                    i = stayEnd + 2
                }
            }
            i++
        }

        return segments
    }

    /** Find the start index of a stationary period ending at or before point index */
    private fun findStayStart(points: List<LocationPointSummary>, endIndex: Int): Int {
        for (i in endIndex downTo 0) {
            if (i + 2 <= points.size) {
                val window = points.slice(i until i + 3)
                if (MovementDetectionEngine.detectStationary(window)) return i
            }
        }
        return endIndex
    }

    /** Find the end index of a stationary period starting at or after point index */
    private fun findStayEnd(points: List<LocationPointSummary>, startIndex: Int): Int {
        for (i in startIndex until points.size) {
            if (i + 2 < points.size) {
                val window = points.slice(i until i + 3)
                if (MovementDetectionEngine.detectStationary(window)) return i
            }
        }
        return startIndex
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
