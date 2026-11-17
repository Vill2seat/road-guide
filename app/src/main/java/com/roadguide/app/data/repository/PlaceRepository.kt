package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.PlaceDao
import com.roadguide.app.data.local.entity.Place
import com.roadguide.app.data.local.entity.PlaceCategory
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    suspend fun insert(place: Place): Long

    suspend fun update(place: Place)

    fun getById(id: Long): Flow<Place?>

    fun observeByCategory(category: PlaceCategory): Flow<List<Place>>

    fun observeMostVisited(): Flow<List<Place>>

    suspend fun deleteById(id: Long)
}

class PlaceRepositoryImpl(private val dao: PlaceDao) : PlaceRepository {

    override suspend fun insert(place: Place): Long = dao.insert(place)

    override suspend fun update(place: Place) = dao.update(place)

    override fun getById(id: Long): Flow<Place?> = dao.getById(id)

    override fun observeByCategory(category: PlaceCategory): Flow<List<Place>> =
        dao.observeByCategory(category)

    override fun observeMostVisited(): Flow<List<Place>> = dao.observeMostVisited()

    override suspend fun deleteById(id: Long) = dao.deleteById(id)
}
ENDTITLE
