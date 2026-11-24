package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.StayDao
import com.roadguide.app.data.local.entity.Stay
import kotlinx.coroutines.flow.Flow

interface StayRepository {
    suspend fun insert(stay: Stay): Long

    suspend fun update(stay: Stay)

    fun observeByRoute(routeId: Long): Flow<List<Stay>>

    fun observeAll(): Flow<List<Stay>>
}

class StayRepositoryImpl(private val dao: StayDao) : StayRepository {

    override suspend fun insert(stay: Stay): Long = dao.insert(stay)

    override suspend fun update(stay: Stay) = dao.update(stay)

    override fun observeByRoute(routeId: Long): Flow<List<Stay>> = dao.observeByRoute(routeId)

    override fun observeAll(): Flow<List<Stay>> = dao.observeAll()
}
