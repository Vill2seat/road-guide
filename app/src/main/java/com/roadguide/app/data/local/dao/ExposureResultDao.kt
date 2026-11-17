package com.roadguide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.roadguide.app.data.local.entity.ExposureResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ExposureResultDao {
    @Query("SELECT * FROM exposure_results WHERE routeId = :routeId ORDER BY createdTimestamp")
    fun getByRouteId(routeId: Long): Flow<List<ExposureResult>>

    @Insert
    suspend fun insert(result: ExposureResult): Long

    @Insert
    suspend fun insertAll(results: List<ExposureResult>): List<Long>

    @Query("SELECT * FROM exposure_results WHERE segmentId = :segmentId LIMIT 1")
    fun getBySegmentId(segmentId: Long): Flow<ExposureResult?>

    @Query("SELECT * FROM exposure_results WHERE stayId = :stayId LIMIT 1")
    fun getByStayId(stayId: Long): Flow<ExposureResult?>

    @Query("SELECT * FROM exposure_results ORDER BY createdTimestamp DESC")
    fun observeAll(): Flow<List<ExposureResult>>
}
