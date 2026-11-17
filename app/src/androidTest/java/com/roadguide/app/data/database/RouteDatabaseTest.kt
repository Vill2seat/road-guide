package com.roadguide.app.data.database

import android.content.Context
import androidx.room.Room
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumentation test for Route database operations.
 * Uses in-memory database for fast, isolated testing.
 */
@RunWith(AndroidJUnit4::class)
class RouteDatabaseTest {

    private lateinit var database: RoadGuideDatabase
    private lateinit var dao: RouteDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            RoadGuideDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.routeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and get route`() {
        val route = Route(
            id = 1L,
            startTime = 1000L,
            endTime = 5000L,
            distanceMeters = 1500.0,
            status = RouteStatus.COMPLETED,
            routeType = RouteType.DAILY_AUTO,
            confidence = 0.95,
            date = "2026-01-01",
            totalDurationSeconds = 3600L
        )

        val insertedId = dao.insert(route)
        assertNotNull(insertedId)

        val fetched = dao.getById(insertedId).first()
        assertNotNull(fetched)
        assertEquals(route.startTime, fetched?.startTime)
        assertEquals(route.distanceMeters, fetched?.distanceMeters, 0.0)
        assertEquals(route.status, fetched?.status)
    }

    @Test
    fun `update route status`() {
        val route = Route(
            id = 1L,
            startTime = 1000L,
            endTime = 5000L,
            distanceMeters = 1500.0,
            status = RouteStatus.IN_PROGRESS,
            routeType = RouteType.DAILY_AUTO,
            confidence = 0.9,
            date = "2026-01-01",
            totalDurationSeconds = 3600L
        )

        dao.insert(route)
        val updated = Route(
            id = 1L,
            startTime = 1000L,
            endTime = 5000L,
            distanceMeters = 1500.0,
            status = RouteStatus.COMPLETED,
            routeType = RouteType.DAILY_AUTO,
            confidence = 0.95,
            date = "2026-01-01",
            totalDurationSeconds = 3600L
        )
        dao.update(updated)

        val fetched = dao.getById(1L).first()
        assertEquals(RouteStatus.COMPLETED, fetched?.status)
    }

    @Test
    fun `delete route`() {
        val route = Route(
            id = 1L,
            startTime = 1000L,
            endTime = 5000L,
            distanceMeters = 1500.0,
            status = RouteStatus.COMPLETED,
            routeType = RouteType.DAILY_AUTO,
            confidence = 0.95,
            date = "2026-01-01",
            totalDurationSeconds = 3600L
        )

        dao.insert(route)
        dao.deleteById(1L)

        val fetched = dao.getById(1L).first()
        assert(fetched == null)
    }
}
