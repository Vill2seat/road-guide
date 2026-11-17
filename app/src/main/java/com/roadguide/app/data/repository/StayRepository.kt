package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.StayDao
import com.roadguide.app.data.local.entity.Stay
import kotlinx.coroutines.flow.Flow

interface StayRepository {
    suspend fun insert(stay: Stay): Long

    suspend fun update(stay: Stay)

    fun getById(id: Long): Flow<Stay?>

    fun getByRouteId(routeId: Long): Flow<List<Stay>>

    fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<Stay>>

    suspend fun delete(id: Long)
}

class StayRepositoryImpl(private val dao: StayDao) : StayRepository {

    override suspend fun insert(stay: Stay): Long = dao.insert(stay)

    override suspend fun update(stay: Stay) = dao.update(stay)

    override fun getById(id: Long): Flow<Stay?> = dao.getById(id)

    override fun getByRouteId(routeId: Long): Flow<List<Stay>> = dao.getByRouteId(routeId)

    override fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<Stay>> =
        dao.getByTimeRange(startTime, endTime)

    override suspend fun delete(id: Long) = dao.delete(id)
}
ENDTITLE
