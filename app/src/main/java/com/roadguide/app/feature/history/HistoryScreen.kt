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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.repository.RouteRepositoryImpl
import com.roadguide.app.domain.usecase.GetRouteSummaries
import com.roadguide.app.domain.usecase.impl.GetRouteSummariesImpl
import com.roadguide.app.feature.history.viewmodel.HistoryViewModel
import com.roadguide.app.mapper.RouteMapper

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataProvider = DataProvider.getInstance(context)
    val routeRepository = RouteRepositoryImpl(dataProvider.routeDao)
    val routeMapper = RouteMapper

    val historyViewModel: HistoryViewModel = viewModel(
        factory = ViewModelProvider.Factory { _ ->
            HistoryViewModel(
                getRouteSummaries = GetRouteSummariesImpl(routeRepository, routeMapper)
            )
        }
    )

    val uiState by historyViewModel.uiState.collectAsState()
    val reports by historyViewModel.reports.collectAsState()

    val currentUiState = uiState

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("History", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentUiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = currentUiState.message,
                    onRetry = { historyViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "${reports.size} reports",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        reports.forEach { report ->
                            Card(modifier = Modifier.fillMaxSize()) {
                                HistoryScreenReportItem(
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
    report: com.roadguide.app.domain.model.RouteSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = report.routeType,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "${report.distanceMeters} m | ${report.durationSeconds}s",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
