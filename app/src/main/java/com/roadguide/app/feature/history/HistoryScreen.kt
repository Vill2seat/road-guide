package com.roadguide.app.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.repository.RouteRepositoryImpl
import com.roadguide.app.data.repository.TrackingSettingsRepositoryImpl
import com.roadguide.app.domain.usecase.GetRouteSummaries
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.domain.usecase.impl.GetRouteSummariesImpl
import com.roadguide.app.domain.usecase.impl.GetTrackingSettingsImpl
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

    val trackingPrefs = TrackingPrefs(context)
    val trackingSettingsRepository = TrackingSettingsRepositoryImpl(trackingPrefs)

    val historyViewModel: HistoryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(
                    getRouteSummaries = GetRouteSummariesImpl(routeRepository, routeMapper),
                    getTrackingSettings = GetTrackingSettingsImpl(trackingSettingsRepository)
                ) as T
            }
        }
    )

    val uiState by historyViewModel.uiState.collectAsState()
    val reports by historyViewModel.reports.collectAsState()

    val screenState = uiState

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (screenState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = screenState.message,
                    onRetry = { historyViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space2))
                        Text(
                            text = "${reports.size} отчётов за период",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space4))

                        if (reports.isEmpty()) {
                            EmptyHistoryState()
                        } else {
                            reports.forEach { report ->
                                HistoryReportItem(
                                    report = report,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(RgDimens.Space3))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RgIcon.RgIconComposable(
            icon = RgIcon.History,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(RgDimens.Space3))
        Text(
            text = "Нет истории",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Отчёты появятся здесь после завершения дней трекинга",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryReportItem(
    report: com.roadguide.app.domain.model.RouteSummary,
    modifier: Modifier = Modifier
) {
    val exposureColor = exposureColorFor(report.averageAqi?.toInt(), MaterialTheme.colorScheme.onSurfaceVariant)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.routeType,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = report.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = RgColor.Success
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column() {
                    Text(
                        text = "${report.distanceMeters.toInt()} м",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Дистанция",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(RgDimens.Space4))

                Column() {
                    Text(
                        text = "${report.durationSeconds / 60} мин",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Длительность",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(RgDimens.Space4))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = report.averageAqi?.let { "${it.toInt()} AQI" } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = exposureColor
                    )
                    Text(
                        text = "Средний AQI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun exposureColorFor(aqi: Int?, onSurfaceVariant: Color): Color {
    return if (aqi == null) {
        onSurfaceVariant
    } else when (aqi) {
        in 0..50 -> RgColor.ExposureGood
        in 51..100 -> RgColor.ExposureModerate
        in 101..150 -> RgColor.ExposureHigh
        else -> RgColor.ExposureVeryHigh
    }
}
