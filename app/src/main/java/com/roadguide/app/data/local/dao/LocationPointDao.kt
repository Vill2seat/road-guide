package com.roadguide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.roadguide.app.data.local.entity.LocationPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(point: LocationPoint): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(points: List<LocationPoint>): List<Long>

    @Delete
    suspend fun delete(point: LocationPoint)

    @Query("SELECT * FROM location_points ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE routeId = :routeId ORDER BY timestamp")
    fun observeByRoute(routeId: Long): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp")
    fun observeInTimeRange(startTime: Long, endTime: Long): Flow<List<LocationPoint>>

    @Query("UPDATE location_points SET routeId = :routeId WHERE id IN (:pointIds)")
    suspend fun assignToRoute(pointIds: List<Long>, routeId: Long)

    @Query("DELETE FROM location_points WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
