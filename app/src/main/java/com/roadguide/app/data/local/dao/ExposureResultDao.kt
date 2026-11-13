package com.roadguide.app.data.local.dao

import com.roadguide.app.data.local.entity.ExposureResult
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface ExposureResultDao {
    @Insert
    suspend fun insertAll(results: List<ExposureResult>): List<Long>

    @Query("SELECT * FROM exposure_results WHERE routeId = :routeId")
    fun getByRouteId(routeId: Long): Flow<List<ExposureResult>>

    @Query("SELECT * FROM exposure_results WHERE segmentId = :segmentId")
    fun getBySegmentId(segmentId: Long): Flow<ExposureResult?>

    @Query("SELECT * FROM exposure_results WHERE stayId = :stayId")
    fun getByStayId(stayId: Long): Flow<ExposureResult?>

    @Query("SELECT * FROM exposure_results ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ExposureResult>>

    @Update
    suspend fun update(result: ExposureResult)

    @Query("DELETE FROM exposure_results WHERE id = :id")
    suspend fun deleteById(id: Long)
}
