package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.domain.usecase.impl.SaveRouteImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SaveRouteImplTest {

    private val mockRouteRepository = object : RouteRepository {
        var lastInsertedRoute: Route? = null

        override suspend fun insert(route: Route): Long {
            lastInsertedRoute = route
            return route.id
        }

        override suspend fun update(route: Route) {}
        override fun getById(id: Long): Flow<Route?> = flow { emit(null) }
        override fun getByDate(date: String): Flow<List<Route>> = flow { emit(emptyList()) }
        override fun observeAll(): Flow<List<Route>> = flow { emit(emptyList()) }
        override fun observeByStatus(status: RouteStatus): Flow<List<Route>> = flow { emit(emptyList()) }
        override fun getRecent(limit: Int): Flow<List<Route>> = flow { emit(emptyList()) }
    }

    private val useCase = SaveRouteImpl(mockRouteRepository)

    @Test
    fun `saveRoute saves route and returns route id`() = kotlinx.coroutines.runBlocking {
        val summary = RouteSummary(
            id = 0,
            startTime = 1000L,
            endTime = 5000L,
            distanceMeters = 1500.0,
            averageAqi = 50.0,
            durationSeconds = 3600L,
            status = "completed",
            routeType = "daily_auto"
        )

        val result = useCase.invoke(summary)

        assertEquals(0L, result)
    }
}
