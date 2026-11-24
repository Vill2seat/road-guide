package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.ExposureResult
import com.roadguide.app.domain.usecase.impl.DailyExposureInput
import com.roadguide.app.domain.usecase.impl.calculateDailyExposureMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyExposureAggregatorTest {

    @Test
    fun `daily score confidence and coverage use duration weighted valid segments`() {
        val metrics = calculateDailyExposureMetrics(
            listOf(
                input(
                    segmentId = 1L,
                    rawScore = 100.0,
                    confidence = 0.8,
                    coverage = 100.0,
                    durationSeconds = 3_600L
                ),
                input(
                    segmentId = 2L,
                    rawScore = 50.0,
                    confidence = 0.4,
                    coverage = 50.0,
                    durationSeconds = 1_800L
                ),
                input(
                    segmentId = 3L,
                    rawScore = 0.0,
                    confidence = 1.0,
                    coverage = 100.0,
                    durationSeconds = 600L
                )
            ),
            totalTrackedDurationSeconds = 6_000L
        )

        assertEquals(83.47011118, metrics?.normalizedScore ?: 0.0, 1e-6)
        assertEquals(66.66666667, metrics?.confidencePercent ?: 0.0, 1e-6)
        assertEquals(90.0, metrics?.coveragePercent ?: 0.0, 1e-6)
        assertEquals(2, metrics?.validInputs?.size)
    }

    @Test
    fun `daily metrics are absent when no valid exposure segment exists`() {
        val metrics = calculateDailyExposureMetrics(
            listOf(
                input(
                    segmentId = 1L,
                    rawScore = 0.0,
                    confidence = 1.0,
                    coverage = 100.0,
                    durationSeconds = 60L
                ),
                input(
                    segmentId = 2L,
                    rawScore = 25.0,
                    confidence = 0.8,
                    coverage = 100.0,
                    durationSeconds = 0L
                )
            ),
            totalTrackedDurationSeconds = 660L
        )

        assertNull(metrics)
    }

    private fun input(
        segmentId: Long,
        rawScore: Double,
        confidence: Double,
        coverage: Double,
        durationSeconds: Long
    ) = DailyExposureInput(
        result = ExposureResult(
            segmentId = segmentId,
            routeId = 1L,
            exposureScore = 50.0,
            rawExposureScore = rawScore,
            pollution = 1.0,
            durationMinutes = 1,
            activityLevel = 1.0,
            confidence = confidence,
            coverage = coverage
        ),
        durationSeconds = durationSeconds
    )
}
