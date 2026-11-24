package com.roadguide.app.feature.dailyreport

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyReportPresentationTest {

    @Test
    fun `exposure score bands map higher exposure to worse color bands`() {
        assertEquals(ExposureScoreBand.LOW, exposureScoreBand(0.0))
        assertEquals(ExposureScoreBand.MODERATE, exposureScoreBand(25.0))
        assertEquals(ExposureScoreBand.HIGH, exposureScoreBand(50.0))
        assertEquals(ExposureScoreBand.VERY_HIGH, exposureScoreBand(75.0))
    }

    @Test
    fun `percentage metrics are displayed as already scaled percentages`() {
        assertEquals("67%", formatDailyPercentage(66.7))
        assertEquals("0%", formatDailyPercentage(0.0))
        assertEquals("100%", formatDailyPercentage(100.0))
        assertEquals("0%", formatDailyPercentage(Double.NaN))
        assertEquals(DataQualityBand.VERY_LOW, dataQualityBand(19.9))
        assertEquals(DataQualityBand.LOW, dataQualityBand(20.0))
        assertEquals(DataQualityBand.MODERATE, dataQualityBand(50.0))
        assertEquals(DataQualityBand.HIGH, dataQualityBand(80.0))
    }
}
