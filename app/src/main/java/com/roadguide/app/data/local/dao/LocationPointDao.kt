package com.roadguide.app.data.local.dao

import com.roadguide.app.data.local.entity.LocationPoint
import androidx.room.DaoObject
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface LocationPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: LocationPoint)

    @Insert
    suspend fun insertAll(points: List<LocationPoint>)

    @Delete
    suspend fun delete(point: LocationPoint)

    @Query("SELECT * FROM location_points ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE routeId = :routeId ORDER BY timestamp")
    fun observeByRoute(routeId: Long): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE timestamp BETWEEN :startTime AND :endTime")
    fun observeInTimeRange(startTime: Long, endTime: Long): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE routeId = :routeId AND qualityFlags & 2 = 0 ORDER BY timestamp LIMIT :limit")
    fun observeRecentGoodQuality(routeId: Long, limit: Int): Flow<List<LocationPoint>>
}
