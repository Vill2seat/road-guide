package com.roadguide.app.data.local.dao

import android.content.Context
import androidx.room.Room
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RouteDaoTest {

    private lateinit var database: RoadGuideDatabase
    private lateinit var dao: RouteDao

    @Before
    fun setUp() {
        val context = com.roadguide.app.util.TestAppContext.getApplicationContext()
        val dbFile = File(context.cacheDir, "test-routing.db")
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
    fun `insert and get route`() = kotlinx.coroutines.runBlocking {
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
    fun `observe all routes`() = kotlinx.coroutines.runBlocking {
        val route1 = Route(
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

        val route2 = Route(
            id = 2L,
            startTime = 2000L,
            endTime = 6000L,
            distanceMeters = 2000.0,
            status = RouteStatus.IN_PROGRESS,
            routeType = RouteType.MANUAL_TRACKING,
            confidence = 0.85,
            date = "2026-01-02",
            totalDurationSeconds = 4000L
        )

        dao.insert(route1)
        dao.insert(route2)

        val allRoutes = dao.observeAll().first()
        assertEquals(2, allRoutes.size)
    }
}
