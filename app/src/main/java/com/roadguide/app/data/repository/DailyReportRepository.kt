package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.DailyReportDao
import com.roadguide.app.data.local.entity.DailyReport
import kotlinx.coroutines.flow.Flow

interface DailyReportRepository {
    suspend fun insert(report: DailyReport): Long

    fun getByDate(date: String): Flow<DailyReport?>

    fun observeRecent(limit: Int): Flow<List<DailyReport>>

    suspend fun update(report: DailyReport)
}

class DailyReportRepositoryImpl(private val dao: DailyReportDao) : DailyReportRepository {

    override suspend fun insert(report: DailyReport): Long = dao.insert(report)

    override fun getByDate(date: String): Flow<DailyReport?> = dao.getByDate(date)

    override fun observeRecent(limit: Int): Flow<List<DailyReport>> = dao.getRecent(limit)

    override suspend fun update(report: DailyReport) = dao.update(report)
}
