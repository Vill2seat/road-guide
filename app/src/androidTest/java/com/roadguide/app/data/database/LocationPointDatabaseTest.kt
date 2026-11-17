package com.roadguide.app.data.database

import android.content.Context
import androidx.room.Room
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.entity.LocationPoint
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
 * Instrumentation test for LocationPoint database operations.
 */
@RunWith(AndroidJUnit4::class)
class LocationPointDatabaseTest {

    private lateinit var database: RoadGuideDatabase
    private lateinit var dao: LocationPointDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            RoadGuideDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.locationPointDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and get location point`() {
        val point = LocationPoint(
            id = 1L,
            latitude = 55.7558,
            longitude = 37.6173,
            timestamp = 1000000000L,
            accuracyMeters = 5f,
            speedMetersPerSecond = 2.5f,
            bearing = 90f,
            altitude = 100.0,
            provider = "fused",
            isMock = false,
            qualityFlags = 0,
            routeId = 1L
        )

        val insertedId = dao.insert(point)
        assertNotNull(insertedId)

        val fetched = dao.getById(insertedId).first()
        assertNotNull(fetched)
        assertEquals(55.7558, fetched?.latitude, 0.0)
        assertEquals(37.6173, fetched?.longitude, 0.0)
        assertEquals(2.5f, fetched?.speedMetersPerSecond ?: 0f, 0.0f)
    }

    @Test
    fun `observe all location points`() {
        val point1 = LocationPoint(
            id = 1L,
            latitude = 55.7558,
            longitude = 37.6173,
            timestamp = 1000000000L,
            accuracyMeters = 5f,
            provider = "fused",
            isMock = false,
            routeId = 1L
        )

        val point2 = LocationPoint(
            id = 2L,
            latitude = 55.7560,
            longitude = 37.6175,
            timestamp = 2000000000L,
            accuracyMeters = 5f,
            provider = "fused",
            isMock = false,
            routeId = 1L
        )

        dao.insert(point1)
        dao.insert(point2)

        val allPoints = dao.observeAll().first()
        assertEquals(2, allPoints.size)
    }
}
