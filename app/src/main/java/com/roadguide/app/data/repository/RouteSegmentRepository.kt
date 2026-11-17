package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.RouteSegmentDao
import com.roadguide.app.data.local.entity.RouteSegment
import kotlinx.coroutines.flow.Flow

interface RouteSegmentRepository {
    suspend fun insert(segment: RouteSegment): Long

    suspend fun update(segment: RouteSegment)

    fun getById(id: Long): Flow<RouteSegment?>

    fun getByRouteId(routeId: Long): Flow<List<RouteSegment>>

    fun getByRouteIdAndTimeRange(routeId: Long, startTime: Long, endTime: Long): Flow<List<RouteSegment>>

    suspend fun delete(id: Long)
}

class RouteSegmentRepositoryImpl(private val dao: RouteSegmentDao) : RouteSegmentRepository {

    override suspend fun insert(segment: RouteSegment): Long = dao.insert(segment)

    override suspend fun update(segment: RouteSegment) = dao.update(segment)

    override fun getById(id: Long): Flow<RouteSegment?> = dao.getById(id)

    override fun getByRouteId(routeId: Long): Flow<List<RouteSegment>> = dao.getByRouteId(routeId)

    override fun getByRouteIdAndTimeRange(
        routeId: Long,
        startTime: Long,
        endTime: Long
    ): Flow<List<RouteSegment>> = dao.getByRouteIdAndTimeRange(routeId, startTime, endTime)

    override suspend fun delete(id: Long) = dao.delete(id)
}
ENDTITLE
