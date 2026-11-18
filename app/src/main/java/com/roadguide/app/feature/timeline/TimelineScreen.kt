package com.roadguide.app.feature.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.components.EmptyState
import com.roadguide.app.core.ui.components.RouteListItem
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.feature.timeline.viewmodel.TimelineViewModel

@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier
) {
    val timelineViewModel: TimelineViewModel = hiltViewModel()

    val uiState by timelineViewModel.uiState.collectAsState()
    val routes by timelineViewModel.routes.collectAsState()

    val screenState = uiState

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (screenState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = screenState.message,
                    onRetry = { timelineViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Daily Timeline",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space2))
                        Text(
                            text = "${routes.size} маршрутов за сегодня",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space4))

                        if (routes.isEmpty()) {
                            EmptyState(
                                icon = RgIcon.Timeline,
                                title = "Нет данных за сегодня",
                                description = "Начните запись в трекере, чтобы увидеть маршруты",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            routes.forEach { route ->
                                RouteListItem(
                                    route = route,
                                    aqiLabel = "Воздействие",
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
