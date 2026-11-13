package com.roadguide.app.data.local.dao

import com.roadguide.app.data.local.entity.PrivacyZone
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface PrivacyZoneDao {
    @Insert
    suspend fun insert(zone: PrivacyZone)

    @Query("SELECT * FROM privacy_zones WHERE isActive = 1")
    fun observeActiveZones(): Flow<List<PrivacyZone>>

    @Query("SELECT * FROM privacy_zones")
    fun observeAll(): Flow<List<PrivacyZone>>

    @Update
    suspend fun update(zone: PrivacyZone)

    @Query("DELETE FROM privacy_zones WHERE id = :id")
    suspend fun deleteById(id: Long)
}
