package com.roadguide.app.data.local.dao

import com.roadguide.app.data.local.entity.Route
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: Route)

    @Query("SELECT * FROM routes WHERE id = :id")
    fun getById(id: Long): Flow<Route?>

    @Query("SELECT * FROM routes WHERE date = :date")
    fun getByDate(date: String): Flow<Route?>

    @Query("SELECT * FROM routes ORDER BY startTime DESC")
    fun observeAll(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE status = :status ORDER BY startTime DESC")
    fun observeByStatus(status: RouteStatus): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE calculationVersion = :version")
    fun observeByVersion(version: Int): Flow<List<Route>>

    @Update
    suspend fun update(route: Route)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
