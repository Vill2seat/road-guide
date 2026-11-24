package com.roadguide.app.service

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.repository.EnvironmentalSampleRepository
import com.roadguide.app.data.repository.ExposureResultRepositoryImpl
import com.roadguide.app.data.repository.LocationPointRepositoryImpl
import com.roadguide.app.data.repository.PrivacyZoneRepositoryImpl
import com.roadguide.app.data.repository.RouteRepositoryImpl
import com.roadguide.app.data.repository.RouteSegmentRepositoryImpl
import com.roadguide.app.data.repository.StayRepositoryImpl
import com.roadguide.app.data.source.ActivityRecognitionSource
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.domain.usecase.impl.TrackingPipeline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackingPipelineIntegrationTest {

    private lateinit var database: RoadGuideDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RoadGuideDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun onLocationReceivedPersistsPointWhenNoPrivacyZonesApply() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pipeline = TrackingPipeline(
            locationSource = LocationSource(context),
            activityRecognitionSource = ActivityRecognitionSource(context),
            locationPointRepository = LocationPointRepositoryImpl(database.locationPointDao()),
            routeRepository = RouteRepositoryImpl(database.routeDao()),
            environmentalSampleRepository = EmptyEnvironmentalSampleRepository,
            routeSegmentRepository = RouteSegmentRepositoryImpl(database.routeSegmentDao()),
            exposureResultRepository = ExposureResultRepositoryImpl(database.exposureResultDao()),
            privacyZoneRepository = PrivacyZoneRepositoryImpl(database.privacyZoneDao()),
            stayRepository = StayRepositoryImpl(database.stayDao())
        )
        val input = LocationPoint(
            latitude = 55.7558,
            longitude = 37.6173,
            timestamp = 1_000L,
            accuracyMeters = 5f,
            speedMetersPerSecond = 1f,
            provider = "test"
        )

        pipeline.onLocationReceived(input)

        val saved = withTimeout(5_000L) {
            database.locationPointDao().observeAll().first { it.isNotEmpty() }.single()
        }
        assertEquals(input.latitude, saved.latitude, 0.0)
        assertEquals(input.longitude, saved.longitude, 0.0)
    }

    private object EmptyEnvironmentalSampleRepository : EnvironmentalSampleRepository {
        override suspend fun insert(sample: EnvironmentalSample): Long = 0L

        override fun observeAll(): Flow<List<EnvironmentalSample>> = flowOf(emptyList())

        override fun getLatest(): Flow<EnvironmentalSample?> = flowOf(null)

        override suspend fun deleteOlderThan(cutoff: Long): Int = 0

        override suspend fun fetchAndSave(
            lat: Double,
            lon: Double,
            requestedTime: Long
        ): EnvironmentalSample? = null

        override suspend fun wasInternetAvailableToday(): Boolean = false
    }
}
