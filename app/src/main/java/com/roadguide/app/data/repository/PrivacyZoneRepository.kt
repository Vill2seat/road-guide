package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.PrivacyZoneDao
import com.roadguide.app.data.local.entity.PrivacyZone
import kotlinx.coroutines.flow.Flow

interface PrivacyZoneRepository {
    suspend fun insert(zone: PrivacyZone): Long

    suspend fun update(zone: PrivacyZone)

    fun observeActiveZones(): Flow<List<PrivacyZone>>

    fun observeAll(): Flow<List<PrivacyZone>>

    suspend fun deleteById(id: Long)
}

class PrivacyZoneRepositoryImpl(private val dao: PrivacyZoneDao) : PrivacyZoneRepository {

    override suspend fun insert(zone: PrivacyZone): Long = dao.insert(zone)

    override suspend fun update(zone: PrivacyZone) = dao.update(zone)

    override fun observeActiveZones(): Flow<List<PrivacyZone>> = dao.observeActiveZones()

    override fun observeAll(): Flow<List<PrivacyZone>> = dao.observeAll()

    override suspend fun deleteById(id: Long) = dao.deleteById(id)
}
