package com.roadguide.app.feature.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.roadguide.app.feature.history.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel = viewModel(
        factory = { HistoryViewModel(
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
    val uiState by historyViewModel.uiState.collectAsState()
    val reports by historyViewModel.reports.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("History", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = uiState.message,
                    onRetry = { historyViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = androidx.compose.foundation.layout.VerticalArrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${reports.size} reports",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        reports.forEach { report ->
                            Card(modifier = Modifier.fillMaxSize()) {
                                com.roadguide.app.feature.history.HistoryScreenReportItem(
                                    report = report,
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
private fun HistoryScreenReportItem(
    report: RouteSummary,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.CenterHorizontally
    ) {
                                Text(
                                    text = report.routeType ?: "Daily Report",
                                    style = MaterialTheme.typography.bodyLarge
                                )
        Text(
            text = "${report.distanceMeters} m | ${report.durationSeconds}s",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
