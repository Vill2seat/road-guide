package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.RouteSegmentDao
import com.roadguide.app.data.local.entity.RouteSegment
import kotlinx.coroutines.flow.Flow

interface RouteSegmentRepository {
    suspend fun insert(segment: RouteSegment): Long

    suspend fun update(segment: RouteSegment)

    fun getById(id: Long): Flow<RouteSegment?>

    fun observeByRoute(routeId: Long): Flow<List<RouteSegment>>
}

class RouteSegmentRepositoryImpl(private val dao: RouteSegmentDao) : RouteSegmentRepository {

    override suspend fun insert(segment: RouteSegment): Long = dao.insert(segment)

    override suspend fun update(segment: RouteSegment) = dao.update(segment)

    override fun getById(id: Long): Flow<RouteSegment?> = dao.getById(id)

    override fun observeByRoute(routeId: Long): Flow<List<RouteSegment>> = dao.observeByRoute(routeId)
}
