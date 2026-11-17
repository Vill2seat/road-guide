package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.DailyReport
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving daily reports.
 */
interface GetDailyReport {
    suspend fun invoke(date: String): DailyReport?

    fun observeRecent(limit: Int): Flow<List<DailyReport>>

    fun observeByDate(date: String): Flow<DailyReport?>
}
