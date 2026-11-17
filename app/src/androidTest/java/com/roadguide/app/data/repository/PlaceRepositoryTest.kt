package com.roadguide.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.entity.Place
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for PlaceRepository.
 * Tests CRUD operations for Places.
 */
@RunWith(AndroidJUnit4::class)
class PlaceRepositoryTest {

    private lateinit var database: RoadGuideDatabase
    private lateinit var repository: PlaceRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            RoadGuideDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        repository = PlaceRepositoryImpl(database.placeDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and get place`() = runBlocking {
        val place = Place(
            id = 1L,
            name = "Test Place",
            address = "123 Test St",
            latitude = 55.7558,
            longitude = 37.6173,
            category = PlaceCategory.PARK,
            isPrivate = false,
            userLabel = "Test",
            visitCount = 1,
            totalDurationSeconds = 300,
            firstVisitTime = 1000L,
            lastVisitTime = 2000L,
            environmentalStats = "{}"
        )

        val insertedId = repository.insert(place)
        assertNotNull(insertedId)

        val fetched = repository.getById(insertedId).first()
        assertNotNull(fetched)
        assertEquals("Test Place", fetched?.name)
        assertEquals(PlaceCategory.PARK, fetched?.category)
    }
}
