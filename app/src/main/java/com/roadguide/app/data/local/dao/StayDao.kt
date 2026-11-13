package com.roadguide.app.data.local.dao

import com.roadguide.app.data.local.entity.Stay
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface StayDao {
    @Insert
    suspend fun insert(stay: Stay)

    @Query("SELECT * FROM stays WHERE routeId = :routeId ORDER BY startTime")
    fun observeByRoute(routeId: Long): Flow<List<Stay>>

    @Query("SELECT * FROM stays")
    fun observeAll(): Flow<List<Stay>>

    @Update
    suspend fun update(stay: Stay)

    @Query("DELETE FROM stays WHERE id = :id")
    suspend fun deleteById(id: Long)
}
