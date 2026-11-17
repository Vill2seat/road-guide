package com.roadguide.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.entity.PrivacyZone
import com.roadguide.app.data.local.entity.PrivacyZoneType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for PrivacyZoneRepository.
 * Tests CRUD operations for PrivacyZones.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyZoneRepositoryTest {

    private lateinit var database: RoadGuideDatabase
    private lateinit var repository: PrivacyZoneRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            RoadGuideDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        repository = PrivacyZoneRepositoryImpl(database.privacyZoneDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and get privacy zone`() = runBlocking {
        val zone = PrivacyZone(
            id = 1L,
            name = "Home Zone",
            centerLatitude = 55.7558,
            centerLongitude = 37.6173,
            radiusMeters = 100.0,
            isActive = true,
            excludeFromHistory = true,
            excludeFromExposure = false,
            createdAt = System.currentTimeMillis()
        )

        val insertedId = repository.insert(zone)
        assertNotNull(insertedId)

        val fetched = repository.getById(insertedId).first()
        assertNotNull(fetched)
        assertEquals("Home Zone", fetched?.name)
        assertEquals(100.0, fetched?.radiusMeters, 0.0)
    }
}
