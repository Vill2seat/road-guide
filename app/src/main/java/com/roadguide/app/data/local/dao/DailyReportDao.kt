package com.roadguide.app.data.local.dao

import com.roadguide.app.data.local.entity.DailyReport
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface DailyReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: DailyReport)

    @Query("SELECT * FROM daily_reports WHERE date = :date")
    fun getByDate(date: String): Flow<DailyReport?>

    @Query("SELECT * FROM daily_reports ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<DailyReport>>

    @Query("SELECT * FROM daily_reports WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRange(start: String, end: String): Flow<List<DailyReport>>

    @Update
    suspend fun update(report: DailyReport)

    @Query("DELETE FROM daily_reports WHERE id = :id")
    suspend fun deleteById(id: Long)
}
