package com.roadguide.app.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roadguide.app.data.local.dao.LocationPointDao
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.entity.LocationPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationPointDatabaseTest {

    private lateinit var database: RoadGuideDatabase
    private lateinit var dao: LocationPointDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RoadGuideDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.locationPointDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertPersistsLocationPoint() = runBlocking {
        val point = LocationPoint(
            latitude = 55.7558,
            longitude = 37.6173,
            timestamp = 1_000L,
            accuracyMeters = 5f,
            speedMetersPerSecond = 2.5f,
            provider = "fused"
        )

        val insertedId = dao.insert(point)
        val saved = dao.observeAll().first().single()

        assertTrue(insertedId > 0L)
        assertEquals(insertedId, saved.id)
        assertEquals(point.latitude, saved.latitude, 0.0)
        assertEquals(point.longitude, saved.longitude, 0.0)
        assertEquals(point.speedMetersPerSecond, saved.speedMetersPerSecond)
    }

    @Test
    fun observeAllReturnsPointsNewestFirst() = runBlocking {
        dao.insert(locationPoint(timestamp = 1_000L))
        dao.insert(locationPoint(timestamp = 2_000L))

        assertEquals(
            listOf(2_000L, 1_000L),
            dao.observeAll().first().map(LocationPoint::timestamp)
        )
    }

    private fun locationPoint(timestamp: Long) = LocationPoint(
        latitude = 55.7558,
        longitude = 37.6173,
        timestamp = timestamp,
        accuracyMeters = 5f,
        provider = "fused"
    )
}
