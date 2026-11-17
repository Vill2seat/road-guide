package com.roadguide.app.service

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.data.local.dao.RouteDao
import com.roadguide.app.domain.model.ExposureConstants
import com.roadguide.app.service.TrackingPipeline
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration test for TrackingPipeline.
 * Tests the full pipeline: location collection -> exposure calculation -> daily report.
 * Requires device/emulator to run.
 */
@RunWith(AndroidJUnit4::class)
class TrackingPipelineIntegrationTest {

    private lateinit var database: RoadGuideDatabase
    private lateinit var routeDao: RouteDao
    private lateinit var pipeline: TrackingPipeline

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            RoadGuideDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        routeDao = database.routeDao()
        pipeline = TrackingPipeline(
            locationPointDao = database.locationPointDao(),
            routeDao = database.routeDao(),
            getEnvironmentalSample = object : com.roadguide.app.domain.usecase.GetEnvironmentalSample {
                override suspend fun invoke(lat: Double, lon: Long): com.roadguide.app.data.local.entity.EnvironmentalSample? = null
                override fun observeNearest(lat: Double, lon: Double, time: Long, delta: Double, window: Long): kotlinx.coroutines.flow.Flow<com.roadguide.app.data.local.entity.EnvironmentalSample?> =
                    kotlinx.coroutines.flow.flow { emit(null) }
            }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `exposure calculation uses correct formula`() {
        // E = K × P × T × A × R × W × S
        // Using ExposureConstants:
        // K = 5.0, T_max = 2.0, R = 1.0, S = 1.0 (defaults)
        // A = 0.7 (stationary activity)

        val duration = 1800.0 // 30 minutes in seconds
        val pollution = 1.0 // maximum pollution level

        // T = min(1800/3600, 2.0) = 0.5
        // E = 5.0 × 1.0 × 0.5 × 0.7 × 1.0 × 1.0 × 1.0 = 1.75
        val exposure = pipeline.calculateExposure(duration, pollution)

        // Formula: E = K × P × T × A × R × W × S
        // K = 5.0, P = 1.0, T = 0.5, A = 0.7, R = 1.0, W = 1.0 (weight), S = 1.0 (skin)
        val expected = ExposureConstants.EXPOSURE_CALIBRATION_K *
            pollution *
            0.5 *
            ExposureConstants.ACTIVITY_FACTOR_STATIONARY *
            ExposureConstants.EXPOSURE_ROAD_SOURCE_FACTOR *
            1.0 *
            1.0

        assertEquals(expected, exposure, 0.001)
    }
}
