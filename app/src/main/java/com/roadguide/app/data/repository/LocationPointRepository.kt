package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.LocationPointDao
import com.roadguide.app.data.local.entity.LocationPoint
import kotlinx.coroutines.flow.Flow

interface LocationPointRepository {
    suspend fun insert(point: LocationPoint): Long

    suspend fun insertAll(points: List<LocationPoint>): List<Long>

    suspend fun delete(point: LocationPoint)

    fun observeAll(): Flow<List<LocationPoint>>

    fun observeByRoute(routeId: Long): Flow<List<LocationPoint>>

    fun observeInTimeRange(startTime: Long, endTime: Long): Flow<List<LocationPoint>>

    suspend fun assignToRoute(pointIds: List<Long>, routeId: Long)

    suspend fun deleteOlderThan(cutoff: Long): Int
}

class LocationPointRepositoryImpl(private val dao: LocationPointDao) : LocationPointRepository {

    override suspend fun insert(point: LocationPoint): Long = dao.insert(point)

    override suspend fun insertAll(points: List<LocationPoint>): List<Long> = dao.insertAll(points)

    override suspend fun delete(point: LocationPoint) = dao.delete(point)

    override fun observeAll(): Flow<List<LocationPoint>> = dao.observeAll()

    override fun observeByRoute(routeId: Long): Flow<List<LocationPoint>> = dao.observeByRoute(routeId)

    override fun observeInTimeRange(startTime: Long, endTime: Long): Flow<List<LocationPoint>> =
        dao.observeInTimeRange(startTime, endTime)

    override suspend fun assignToRoute(pointIds: List<Long>, routeId: Long) =
        dao.assignToRoute(pointIds, routeId)

    override suspend fun deleteOlderThan(cutoff: Long): Int = dao.deleteOlderThan(cutoff)
}
