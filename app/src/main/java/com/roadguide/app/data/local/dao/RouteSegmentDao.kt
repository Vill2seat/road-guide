package com.roadguide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.roadguide.app.data.local.entity.RouteSegment
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteSegmentDao {
    @Insert
    suspend fun insert(segment: RouteSegment): Long

    @Insert
    suspend fun insertAll(segments: List<RouteSegment>): List<Long>

    @Update
    suspend fun update(segment: RouteSegment)

    @Query("SELECT * FROM route_segments WHERE routeId = :routeId ORDER BY startTime")
    fun observeByRoute(routeId: Long): Flow<List<RouteSegment>>

    @Query("SELECT * FROM route_segments WHERE id = :id")
    fun getById(id: Long): Flow<RouteSegment?>

    @Query("UPDATE route_segments SET endTime = :endTime WHERE id = :id")
    suspend fun updateEndTime(id: Long, endTime: Long)
}
