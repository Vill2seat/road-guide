package com.roadguide.app.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roadguide.app.data.local.dao.RouteDao
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteDatabaseTest {

    private lateinit var database: RoadGuideDatabase
    private lateinit var dao: RouteDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RoadGuideDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.routeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetRoute() = runBlocking {
        val route = route()
        val insertedId = dao.insert(route)
        val fetched = dao.getById(insertedId).first()

        assertTrue(insertedId > 0L)
        assertEquals(route.startTime, fetched?.startTime)
        assertEquals(route.distanceMeters, fetched?.distanceMeters)
        assertEquals(route.status, fetched?.status)
    }

    @Test
    fun updatePersistsRouteStatus() = runBlocking {
        val insertedId = dao.insert(route())
        val updated = route(id = insertedId, status = RouteStatus.COMPLETED)

        dao.update(updated)

        assertEquals(RouteStatus.COMPLETED, dao.getById(insertedId).first()?.status)
    }

    @Test
    fun getByDateReturnsMatchingRoutes() = runBlocking {
        dao.insert(route(date = "2026-01-01"))
        dao.insert(route(date = "2026-01-02", startTime = 2_000L))

        assertEquals(
            listOf("2026-01-01"),
            dao.getByDate("2026-01-01").first().map(Route::date)
        )
    }

    private fun route(
        id: Long = 0L,
        status: RouteStatus = RouteStatus.IN_PROGRESS,
        date: String = "2026-01-01",
        startTime: Long = 1_000L
    ) = Route(
        id = id,
        startTime = startTime,
        endTime = 5_000L,
        distanceMeters = 1_500.0,
        status = status,
        routeType = RouteType.DAILY_AUTO,
        confidence = 0.95,
        date = date,
        totalDurationSeconds = 3_600L
    )
}
