package com.roadguide.app.service

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.domain.model.ExposureConstants
import com.roadguide.app.service.TrackingPipeline
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration test for exposure calculation formula.
 * Verifies E = K × P × T × A × R × W × S using TrackingPipeline.
 */
@RunWith(RobolectricTestRunner::class)
class ExposureCalculationIntegrationTest {

    private lateinit var database: RoadGuideDatabase
    private lateinit var pipeline: TrackingPipeline

    @org.junit.Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            RoadGuideDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
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

    @org.junit.After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `exposure calculation matches formula E = K x P x T x A x R x W x S`() {
        // E = K × P × T × A × R × W × S
        // K = 5.0, P = 1.0, T = min(1800/3600, 2.0) = 0.5
        // A = 0.7 (stationary), R = 1.0, W = 1.0, S = 1.0
        val exposure = pipeline.calculateExposure(
            durationSeconds = 1800L,
            exposureLevel = 1.0,
            skinFactor = ExposureConstants.DEFAULT_SKIN_FACTOR,
            screenFactor = ExposureConstants.DEFAULT_SCREEN_FACTOR
        )

        val expected = ExposureConstants.EXPOSURE_CALIBRATION_K *
            1.0 *
            0.5 *
            ExposureConstants.ACTIVITY_FACTOR_STATIONARY *
            ExposureConstants.EXPOSURE_ROAD_SOURCE_FACTOR *
            ExposureConstants.DEFAULT_SKIN_FACTOR *
            ExposureConstants.DEFAULT_SCREEN_FACTOR

        assertEquals(expected, exposure, 0.001)
    }

    @Test
    fun `exposure T does not exceed T_MAX`() {
        // T = min(7200/3600, 2.0) = 2.0 (capped at T_MAX)
        val exposure = pipeline.calculateExposure(
            durationSeconds = 7200L,
            exposureLevel = 1.0,
            skinFactor = ExposureConstants.DEFAULT_SKIN_FACTOR,
            screenFactor = ExposureConstants.DEFAULT_SCREEN_FACTOR
        )

        val expected = ExposureConstants.EXPOSURE_CALIBRATION_K *
            1.0 *
            ExposureConstants.EXPOSURE_T_MAX *
            ExposureConstants.ACTIVITY_FACTOR_STATIONARY *
            ExposureConstants.EXPOSURE_ROAD_SOURCE_FACTOR *
            ExposureConstants.DEFAULT_SKIN_FACTOR *
            ExposureConstants.DEFAULT_SCREEN_FACTOR

        assertEquals(expected, exposure, 0.001)
    }
}
