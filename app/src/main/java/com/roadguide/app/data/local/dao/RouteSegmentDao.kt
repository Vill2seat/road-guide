package com.roadguide.app.data.local.dao

import com.roadguide.app.data.local.entity.RouteSegment
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface RouteSegmentDao {
    @Insert
    suspend fun insert(segment: RouteSegment)

    @Query("SELECT * FROM route_segments WHERE routeId = :routeId ORDER BY startTime")
    fun observeByRoute(routeId: Long): Flow<List<RouteSegment>>

    @Query("SELECT * FROM route_segments WHERE routeId = :routeId AND startTime BETWEEN :start AND :end")
    fun observeByRouteInTimeRange(routeId: Long, start: Long, end: Long): Flow<List<RouteSegment>>

    @Update
    suspend fun update(segment: RouteSegment)

    @Query("DELETE FROM route_segments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
