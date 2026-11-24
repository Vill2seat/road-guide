package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.PrivacyStatus
import com.roadguide.app.data.local.entity.PrivacyZone
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.data.local.entity.Stay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class PrivacyScope { HISTORY, EXPOSURE }

data class PrivacyDecision(
    val excludeFromHistory: Boolean,
    val excludeFromExposure: Boolean
)

data class ExportableLocationPoint(
    val locationPoint: LocationPoint,
    val includeExposure: Boolean
)

object PrivacyZonePolicy {

    const val MAX_TRACKED_GAP_MS = 5 * 60 * 1_000L

    fun decisionAt(
        latitude: Double,
        longitude: Double,
        zones: List<PrivacyZone>
    ): PrivacyDecision {
        if (!hasValidCoordinates(latitude, longitude)) {
            return PrivacyDecision(
                excludeFromHistory = zones.any { it.isActive && it.excludeFromHistory },
                excludeFromExposure = zones.any { it.isActive && it.excludeFromExposure }
            )
        }
        val activeZones = zones.filter { it.isActive }
        val invalidZones = activeZones.filter { zone ->
            !hasValidCoordinates(zone.centerLatitude, zone.centerLongitude) ||
                !zone.radiusMeters.isFinite()
        }
        val matchedZones = activeZones.filter { zone ->
            contains(zone, latitude, longitude)
        }
        return PrivacyDecision(
            excludeFromHistory = matchedZones.any { it.excludeFromHistory } ||
                invalidZones.any { it.excludeFromHistory },
            excludeFromExposure = matchedZones.any { it.excludeFromExposure } ||
                invalidZones.any { it.excludeFromExposure }
        )
    }

    fun excludesPoint(
        point: LocationPoint,
        zones: List<PrivacyZone>,
        scope: PrivacyScope
    ): Boolean {
        if (scope == PrivacyScope.EXPOSURE && point.excludeFromExposure) return true
        if (
            (!point.latitude.isFinite() || !point.longitude.isFinite()) &&
            zones.any { it.isActive && excludes(it, scope) }
        ) return true
        val decision = decisionAt(point.latitude, point.longitude, zones)
        return when (scope) {
            PrivacyScope.HISTORY -> decision.excludeFromHistory
            PrivacyScope.EXPOSURE -> decision.excludeFromExposure
        }
    }

    fun excludesPath(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        zones: List<PrivacyZone>,
        scope: PrivacyScope
    ): Boolean {
        val applicableZones = zones.filter { it.isActive && excludes(it, scope) }
        if (applicableZones.isEmpty()) return false
        if (
            !hasValidCoordinates(startLatitude, startLongitude) ||
            !hasValidCoordinates(endLatitude, endLongitude)
        ) return true
        if (applicableZones.any { zone ->
                !hasValidCoordinates(zone.centerLatitude, zone.centerLongitude) ||
                    !zone.radiusMeters.isFinite()
            }
        ) return true

        return applicableZones.any { zone ->
            distanceToPathMeters(
                zone.centerLatitude,
                zone.centerLongitude,
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude
            ) <= zone.radiusMeters.coerceAtLeast(0.0)
        }
    }

    private fun hasValidCoordinates(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() &&
            latitude in -90.0..90.0 &&
            longitude.isFinite() &&
            longitude in -180.0..180.0

    fun excludesSegment(
        segment: RouteSegment,
        points: List<LocationPoint>,
        zones: List<PrivacyZone>,
        scope: PrivacyScope
    ): Boolean {
        if (
            excludesPath(
                segment.startLatitude,
                segment.startLongitude,
                segment.endLatitude,
                segment.endLongitude,
                zones,
                scope
            )
        ) return true

        return points.any { point ->
            point.timestamp in segment.startTime..segment.endTime &&
                excludesPoint(point, zones, scope)
        }
    }

    fun excludesStay(
        stay: Stay,
        zones: List<PrivacyZone>,
        scope: PrivacyScope
    ): Boolean {
        if (stay.privacyStatus != PrivacyStatus.PUBLIC) return true
        val applicableZones = zones.filter { it.isActive && excludes(it, scope) }
        if (applicableZones.isEmpty()) return false
        if (
            !hasValidCoordinates(stay.centerLatitude, stay.centerLongitude) ||
            !stay.radiusMeters.isFinite() ||
            applicableZones.any { zone ->
                !hasValidCoordinates(zone.centerLatitude, zone.centerLongitude) ||
                    !zone.radiusMeters.isFinite()
            }
        ) return true

        return applicableZones.any { zone ->
            distanceMeters(
                stay.centerLatitude,
                stay.centerLongitude,
                zone.centerLatitude,
                zone.centerLongitude
            ) <= stay.radiusMeters.coerceAtLeast(0.0) + zone.radiusMeters.coerceAtLeast(0.0)
        }
    }

    fun filterPoints(
        points: List<LocationPoint>,
        zones: List<PrivacyZone>,
        scope: PrivacyScope
    ): List<LocationPoint> = points.filterNot { excludesPoint(it, zones, scope) }

    fun prepareForExport(
        points: List<LocationPoint>,
        zones: List<PrivacyZone>
    ): List<ExportableLocationPoint> =
        filterPoints(points, zones, PrivacyScope.HISTORY).map { point ->
            ExportableLocationPoint(
                locationPoint = point,
                includeExposure = !excludesPoint(point, zones, PrivacyScope.EXPOSURE)
            )
        }

    fun recordedDistanceMeters(
        points: List<LocationPoint>,
        zones: List<PrivacyZone>
    ): Double = orderedDistinctPoints(points).zipWithNext().sumOf { (previous, current) ->
        if (
            current.timestamp - previous.timestamp > MAX_TRACKED_GAP_MS ||
            excludesPath(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude,
                zones,
                PrivacyScope.HISTORY
            )
        ) {
            0.0
        } else {
            distanceMeters(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
        }
    }

    fun recordedDurationSeconds(
        points: List<LocationPoint>,
        zones: List<PrivacyZone>
    ): Long = orderedDistinctPoints(points).zipWithNext().sumOf { (previous, current) ->
        val durationMillis = current.timestamp - previous.timestamp
        if (
            durationMillis <= 0L ||
            durationMillis > MAX_TRACKED_GAP_MS ||
            excludesPath(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude,
                zones,
                PrivacyScope.HISTORY
            )
        ) {
            0L
        } else {
            durationMillis / 1_000L
        }
    }

    private fun excludes(zone: PrivacyZone, scope: PrivacyScope): Boolean = when (scope) {
        PrivacyScope.HISTORY -> zone.excludeFromHistory
        PrivacyScope.EXPOSURE -> zone.excludeFromExposure
    }

    private fun contains(zone: PrivacyZone, latitude: Double, longitude: Double): Boolean =
        hasValidCoordinates(latitude, longitude) &&
            hasValidCoordinates(zone.centerLatitude, zone.centerLongitude) &&
            distanceMeters(
                zone.centerLatitude,
                zone.centerLongitude,
                latitude,
                longitude
            ) <= zone.radiusMeters.coerceAtLeast(0.0)

    private fun distanceToPathMeters(
        centerLatitude: Double,
        centerLongitude: Double,
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        val metersPerRadian = 6_371_000.0
        val latitudeScale = cos(Math.toRadians(centerLatitude)).coerceAtLeast(1e-6)
        val startX = longitudeDeltaRadians(centerLongitude, startLongitude) *
            metersPerRadian * latitudeScale
        val startY = Math.toRadians(startLatitude - centerLatitude) * metersPerRadian
        val endX = longitudeDeltaRadians(centerLongitude, endLongitude) *
            metersPerRadian * latitudeScale
        val endY = Math.toRadians(endLatitude - centerLatitude) * metersPerRadian
        val deltaX = endX - startX
        val deltaY = endY - startY
        val lengthSquared = deltaX * deltaX + deltaY * deltaY
        val fraction = if (lengthSquared == 0.0) {
            0.0
        } else {
            (-(startX * deltaX + startY * deltaY) / lengthSquared).coerceIn(0.0, 1.0)
        }
        val nearestX = startX + fraction * deltaX
        val nearestY = startY + fraction * deltaY
        return sqrt(nearestX * nearestX + nearestY * nearestY)
    }

    private fun longitudeDeltaRadians(origin: Double, destination: Double): Double {
        val deltaDegrees = ((destination - origin + 540.0) % 360.0) - 180.0
        return Math.toRadians(deltaDegrees)
    }

    private fun distanceMeters(
        firstLatitude: Double,
        firstLongitude: Double,
        secondLatitude: Double,
        secondLongitude: Double
    ): Double {
        val latitudeDelta = Math.toRadians(secondLatitude - firstLatitude)
        val longitudeDelta = longitudeDeltaRadians(firstLongitude, secondLongitude)
        val firstLatitudeRadians = Math.toRadians(firstLatitude)
        val secondLatitudeRadians = Math.toRadians(secondLatitude)
        val haversine = (
            sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(firstLatitudeRadians) * cos(secondLatitudeRadians) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
            ).coerceIn(0.0, 1.0)
        return 2 * 6_371_000.0 * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    private fun orderedDistinctPoints(points: List<LocationPoint>): List<LocationPoint> =
        points.sortedBy(LocationPoint::timestamp).distinctBy(LocationPoint::timestamp)
}
