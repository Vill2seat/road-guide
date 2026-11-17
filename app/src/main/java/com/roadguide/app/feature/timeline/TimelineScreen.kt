package com.roadguide.app.feature.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.feature.timeline.viewmodel.TimelineViewModel

@Composable
fun TimelineScreen(
    timelineViewModel: TimelineViewModel = viewModel(
        factory = { TimelineViewModel(
            getRouteSummaries = com.roadguide.app.domain.usecase.impl.GetRouteSummariesImpl(
                routeRepository = com.roadguide.app.data.repository.RouteRepositoryImpl(
                    dao = com.roadguide.app.data.local.db.RoadGuideDatabase.getInstance(
                        androidx.lifecycle.viewmodel.viewModelScopeLocalContext.current.context
                    ).routeDao
                )
            )
        ) }
    ),
    modifier: Modifier = Modifier
) {
    val uiState by timelineViewModel.uiState.collectAsState()
    val routes by timelineViewModel.routes.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Timeline", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = uiState.message,
                    onRetry = { timelineViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = androidx.compose.foundation.layout.VerticalArrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${routes.size} routes",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        routes.forEach { route ->
                            Card(modifier = Modifier.fillMaxSize()) {
                                com.roadguide.app.feature.timeline.TimelineScreenRouteItem(
                                    route = route,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineScreenRouteItem(
    route: RouteSummary,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.CenterHorizontally
    ) {
                                Text(
                                    text = route.routeType ?: "Route",
                                    style = MaterialTheme.typography.bodyLarge
                                )
        Text(
            text = "${route.distanceMeters} m",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
