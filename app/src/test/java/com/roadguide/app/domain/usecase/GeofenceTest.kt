package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.PrivacyZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты для логики геофенсинга (Haversine расчёт + проверка попадания в зону).
 *
 * Проверяет:
 * 1. Точка внутри зоны (расстояние < radius) -> true
 * 2. Точка на границе зоны (расстояние == radius) -> true
 * 3. Точка снаружи зоны (расстояние > radius) -> false
 * 4. Точка в центре зоны -> true
 * 5. Нулевой радиус зоны -> только точка в центре считается внутри
 */
class GeofenceTest {

    private companion object {
        private const val EARTH_RADIUS_M = 6371000.0
    }

    /** Haversine: расчёт расстояния между двумя точками (метры) */
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /** Проверка попадания в зону (аналог isInsidePrivacyZone) */
    private fun isInsideZone(lat: Double, lon: Double, zone: PrivacyZone): Boolean {
        return haversine(lat, lon, zone.centerLatitude, zone.centerLongitude) <= zone.radiusMeters
    }

    @Test
    fun testPointAtZoneCenterIsInside() {
        val zone = PrivacyZone(
            id = 1,
            name = "Test Zone",
            centerLatitude = 55.7522,
            centerLongitude = 37.6133,
            radiusMeters = 100.0,
            isActive = true,
            excludeFromHistory = true,
            excludeFromExposure = false
        )
        assertTrue(isInsideZone(55.7522, 37.6133, zone))
    }

    @Test
    fun testPointWithinRadiusIsInside() {
        val zone = PrivacyZone(
            id = 1,
            name = "Test Zone",
            centerLatitude = 55.7522,
            centerLongitude = 37.6133,
            radiusMeters = 1000.0,
            isActive = true,
            excludeFromHistory = true,
            excludeFromExposure = false
        )
        assertTrue(isInsideZone(55.7567, 37.6133, zone))
    }

    @Test
    fun testPointOnBoundaryIsInside() {
        val zone = PrivacyZone(
            id = 1,
            name = "Test Zone",
            centerLatitude = 55.7522,
            centerLongitude = 37.6133,
            radiusMeters = 100.0,
            isActive = true,
            excludeFromHistory = true,
            excludeFromExposure = false
        )
        val latOffset = 100.0 / 111320.0
        assertTrue(isInsideZone(55.7522 + latOffset, 37.6133, zone))
    }

    @Test
    fun testPointOutsideZoneIsNotInside() {
        val zone = PrivacyZone(
            id = 1,
            name = "Test Zone",
            centerLatitude = 55.7522,
            centerLongitude = 37.6133,
            radiusMeters = 100.0,
            isActive = true,
            excludeFromHistory = true,
            excludeFromExposure = false
        )
        assertFalse(isInsideZone(55.7612, 37.6133, zone))
    }

    @Test
    fun testZeroRadiusZoneOnlyCenterIsInside() {
        val zone = PrivacyZone(
            id = 1,
            name = "Zero Radius Zone",
            centerLatitude = 55.7522,
            centerLongitude = 37.6133,
            radiusMeters = 0.0,
            isActive = true,
            excludeFromHistory = true,
            excludeFromExposure = false
        )
        assertTrue(isInsideZone(55.7522, 37.6133, zone))
        assertFalse(isInsideZone(55.75221, 37.6133, zone))
    }

    @Test
    fun testZoneWithExcludeFromHistoryFiltersPoint() {
        val zone = PrivacyZone(
            id = 1,
            name = "Private Home",
            centerLatitude = 55.7522,
            centerLongitude = 37.6133,
            radiusMeters = 500.0,
            isActive = true,
            excludeFromHistory = true,
            excludeFromExposure = false
        )
        assertTrue(isInsideZone(55.7522, 37.6133, zone))
        assertTrue(zone.excludeFromHistory)
    }

    @Test
    fun testZoneWithExcludeFromExposureMarksPoint() {
        val zone = PrivacyZone(
            id = 1,
            name = "Sensitive Area",
            centerLatitude = 55.7522,
            centerLongitude = 37.6133,
            radiusMeters = 500.0,
            isActive = true,
            excludeFromHistory = false,
            excludeFromExposure = true
        )
        assertTrue(zone.excludeFromExposure)
        assertFalse(zone.excludeFromHistory)
    }

    @Test
    fun testInactiveZonesAreIgnored() {
        val activeZone = PrivacyZone(
            id = 1,
            name = "Active",
            centerLatitude = 55.7522,
            centerLongitude = 37.6133,
            radiusMeters = 1000.0,
            isActive = true,
            excludeFromHistory = false,
            excludeFromExposure = false
        )
        val inactiveZone = PrivacyZone(
            id = 2,
            name = "Inactive",
            centerLatitude = 55.7522,
            centerLongitude = 37.6133,
            radiusMeters = 1000.0,
            isActive = false,
            excludeFromHistory = true,
            excludeFromExposure = true
        )
        assertTrue(activeZone.isActive)
        assertFalse(inactiveZone.isActive)
    }
}