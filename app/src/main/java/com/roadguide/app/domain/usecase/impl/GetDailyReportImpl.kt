package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.DailyReport
import com.roadguide.app.data.repository.DailyReportRepository
import com.roadguide.app.domain.usecase.GetDailyReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take

class GetDailyReportImpl(
    private val dailyReportRepository: DailyReportRepository
) : GetDailyReport {

    override suspend fun invoke(date: String): DailyReport? {
        return dailyReportRepository.getByDate(date).take(1).firstOrNull()
    }

    override fun observeRecent(limit: Int): Flow<List<DailyReport>> {
        return dailyReportRepository.observeRecent(limit)
    }

    override fun observeByDate(date: String): Flow<DailyReport?> {
        return dailyReportRepository.getByDate(date)
    }
}
