package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.PrivacyZoneDao
import com.roadguide.app.data.local.entity.PrivacyZone
import kotlinx.coroutines.flow.Flow

interface PrivacyZoneRepository {
    suspend fun insert(zone: PrivacyZone): Long

    suspend fun update(zone: PrivacyZone)

    fun getById(id: Long): Flow<PrivacyZone?>

    fun observeActive(): Flow<List<PrivacyZone>>

    suspend fun deleteById(id: Long)
}

class PrivacyZoneRepositoryImpl(private val dao: PrivacyZoneDao) : PrivacyZoneRepository {

    override suspend fun insert(zone: PrivacyZone): Long = dao.insert(zone)

    override suspend fun update(zone: PrivacyZone) = dao.update(zone)

    override fun getById(id: Long): Flow<PrivacyZone?> = dao.getById(id)

    override fun observeActive(): Flow<List<PrivacyZone>> = dao.observeActive()

    override suspend fun deleteById(id: Long) = dao.deleteById(id)
}
ENDTITLE
