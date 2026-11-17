package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.RouteDao
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    suspend fun insert(route: Route): Long

    suspend fun update(route: Route)

    fun getById(id: Long): Flow<Route?>

    fun getByDate(date: String): Flow<List<Route>>

    fun observeAll(): Flow<List<Route>>

    fun observeByStatus(status: RouteStatus): Flow<List<Route>>

    fun getRecent(limit: Int): Flow<List<Route>>
}

class RouteRepositoryImpl(private val dao: RouteDao) : RouteRepository {

    override suspend fun insert(route: Route): Long = dao.insert(route)

    override suspend fun update(route: Route) = dao.update(route)

    override fun getById(id: Long): Flow<Route?> = dao.getById(id)

    override fun getByDate(date: String): Flow<List<Route>> = dao.getByDate(date)

    override fun observeAll(): Flow<List<Route>> = dao.observeAll()

    override fun observeByStatus(status: RouteStatus): Flow<List<Route>> = dao.observeByStatus(status)

    override fun getRecent(limit: Int): Flow<List<Route>> = dao.getRecent(limit)
}
ENDTITLE
