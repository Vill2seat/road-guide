package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.EnvironmentalSampleDao
import com.roadguide.app.data.local.entity.EnvironmentalSample
import kotlinx.coroutines.flow.Flow

interface EnvironmentalSampleRepository {
    suspend fun insert(sample: EnvironmentalSample): Long

    fun observeAll(): Flow<List<EnvironmentalSample>>

    fun observeByRouteId(routeId: Long): Flow<List<EnvironmentalSample>>

    fun getLatest(): Flow<EnvironmentalSample?>

    suspend fun deleteOlderThan(cutoff: Long): Int
}

class EnvironmentalSampleRepositoryImpl(private val dao: EnvironmentalSampleDao) :
    EnvironmentalSampleRepository {

    override suspend fun insert(sample: EnvironmentalSample): Long = dao.insert(sample)

    override fun observeAll(): Flow<List<EnvironmentalSample>> = dao.observeAll()

    override fun observeByRouteId(routeId: Long): Flow<List<EnvironmentalSample>> =
        dao.observeByRouteId(routeId)

    override fun getLatest(): Flow<EnvironmentalSample?> = dao.getLatest()

    override suspend fun deleteOlderThan(cutoff: Long): Int = dao.deleteOlderThan(cutoff)
}
ENDTITLE
