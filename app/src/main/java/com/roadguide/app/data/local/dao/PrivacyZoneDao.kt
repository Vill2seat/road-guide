package com.roadguide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roadguide.app.data.local.entity.PrivacyZone
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivacyZoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zone: PrivacyZone): Long

    @Update
    suspend fun update(zone: PrivacyZone)

    @Query("SELECT * FROM privacy_zones WHERE isActive = 1")
    fun observeActiveZones(): Flow<List<PrivacyZone>>

    @Query("SELECT * FROM privacy_zones ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PrivacyZone>>

    @Query("DELETE FROM privacy_zones WHERE id = :id")
    suspend fun deleteById(id: Long)
}
