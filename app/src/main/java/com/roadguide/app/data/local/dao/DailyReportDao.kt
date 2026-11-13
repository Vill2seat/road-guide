package com.roadguide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roadguide.app.data.local.entity.DailyReport
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: DailyReport)

    @Update
    suspend fun update(report: DailyReport)

    @Query("SELECT * FROM daily_reports WHERE date = :date LIMIT 1")
    fun getByDate(date: String): Flow<DailyReport?>

    @Query("SELECT * FROM daily_reports ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<DailyReport>>

    @Query("SELECT * FROM daily_reports WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRange(start: String, end: String): Flow<List<DailyReport>>
}
