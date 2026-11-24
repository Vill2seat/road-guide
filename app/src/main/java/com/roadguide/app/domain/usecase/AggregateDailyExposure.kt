package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.DailyReport

/** Interface for daily exposure aggregation use case. */
interface AggregateDailyExposure {
    /**
     * Aggregates all tracking data for the given date into a DailyReport.
     * Returns null if no valid exposure segments exist for the date.
     */
    suspend fun aggregate(date: String): DailyReport?

    /**
     * Regenerates the DailyReport for the given date (e.g. when late data arrives).
     * Returns null if no valid exposure segments exist for the date.
     */
    suspend fun regenerate(date: String): DailyReport?
}