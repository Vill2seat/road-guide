package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.ExposureResultDao
import com.roadguide.app.data.local.entity.ExposureResult
import kotlinx.coroutines.flow.Flow

interface ExposureResultRepository {
    suspend fun insert(result: ExposureResult): Long

    suspend fun update(result: ExposureResult)

    fun getById(id: Long): Flow<ExposureResult?>

    fun getByRouteId(routeId: Long): Flow<List<ExposureResult>>

    fun getBySegmentId(segmentId: Long): Flow<List<ExposureResult>>

    fun getByStayId(stayId: Long): Flow<List<ExposureResult>>

    suspend fun deleteById(id: Long)
}

class ExposureResultRepositoryImpl(private val dao: ExposureResultDao) : ExposureResultRepository {

    override suspend fun insert(result: ExposureResult): Long = dao.insert(result)

    override suspend fun update(result: ExposureResult) = dao.update(result)

    override fun getById(id: Long): Flow<ExposureResult?> = dao.getById(id)

    override fun getByRouteId(routeId: Long): Flow<List<ExposureResult>> =
        dao.getByRouteId(routeId)

    override fun getBySegmentId(segmentId: Long): Flow<List<ExposureResult>> =
        dao.getBySegmentId(segmentId)

    override fun getByStayId(stayId: Long): Flow<List<ExposureResult>> =
        dao.getByStayId(stayId)

    override suspend fun deleteById(id: Long) = dao.deleteById(id)
}
ENDTITLE
