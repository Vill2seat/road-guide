package com.roadguide.app.data.local.dao

import com.roadguide.app.data.local.entity.Place
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: Place)

    @Query("SELECT * FROM places WHERE id = :id")
    fun getById(id: Long): Flow<Place?>

    @Query("SELECT * FROM places WHERE category = :category")
    fun observeByCategory(category: PlaceCategory): Flow<List<Place>>

    @Query("SELECT * FROM places ORDER BY visitCount DESC")
    fun observeMostVisited(limit: Int): Flow<List<Place>>

    @Update
    suspend fun update(place: Place)

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deleteById(id: Long)
}
