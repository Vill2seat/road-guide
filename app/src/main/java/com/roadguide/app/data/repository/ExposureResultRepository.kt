package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.ExposureResultDao
import com.roadguide.app.data.local.entity.ExposureResult
import kotlinx.coroutines.flow.Flow

interface ExposureResultRepository {
    suspend fun insert(result: ExposureResult): Long

    fun getByRouteId(routeId: Long): Flow<List<ExposureResult>>

    fun getBySegmentId(segmentId: Long): Flow<ExposureResult?>

    fun getByStayId(stayId: Long): Flow<ExposureResult?>

    fun observeAll(): Flow<List<ExposureResult>>
}

class ExposureResultRepositoryImpl(private val dao: ExposureResultDao) : ExposureResultRepository {

    override suspend fun insert(result: ExposureResult): Long = dao.insert(result)

    override fun getByRouteId(routeId: Long): Flow<List<ExposureResult>> =
        dao.getByRouteId(routeId)

    override fun getBySegmentId(segmentId: Long): Flow<ExposureResult?> =
        dao.getBySegmentId(segmentId)

    override fun getByStayId(stayId: Long): Flow<ExposureResult?> =
        dao.getByStayId(stayId)

    override fun observeAll(): Flow<List<ExposureResult>> = dao.observeAll()
}
