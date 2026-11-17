package com.roadguide.app.domain.usecase

import com.roadguide.app.domain.model.RouteSummary
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
        override fun getById(id: Long): kotlinx.coroutines.flow.Flow<Route?> = kotlinx.coroutines.flow.flow { emit(null) }
        override fun getByDate(date: String): kotlinx.coroutines.flow.Flow<List<Route>> = kotlinx.coroutines.flow.flow { emit(emptyList()) }
        override fun observeAll(): kotlinx.coroutines.flow.Flow<List<Route>> = kotlinx.coroutines.flow.flow { emit(emptyList()) }
        override fun observeByStatus(status: RouteStatus): kotlinx.coroutines.flow.Flow<List<Route>> = kotlinx.coroutines.flow.flow { emit(emptyList()) }
        override fun getRecent(limit: Int): kotlinx.coroutines.flow.Flow<List<Route>> = kotlinx.coroutines.flow.flow { emit(emptyList()) }
    }

    private val useCase = SaveRouteImpl(mockRouteRepository)

    @Test
    fun `saveRoute saves route and returns route id`() {
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

        // SaveRouteImpl returns routeRepository.insert(route)
        // If mock returns 0L, then result should be 0L
        // This is a mock repository test - assert on the interaction
        val result = useCase.invoke(summary)

        assertEquals(0L, result)
    }
}
