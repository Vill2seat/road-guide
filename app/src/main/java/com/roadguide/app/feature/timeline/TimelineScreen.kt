package com.roadguide.app.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.components.EmptyState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.domain.model.RouteTimeline
import com.roadguide.app.domain.model.TimelineEvent
import com.roadguide.app.feature.timeline.viewmodel.TimelineViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier,
    onOpenDailyReport: () -> Unit = {}
) {
    val timelineViewModel: TimelineViewModel = hiltViewModel()
    val uiState by timelineViewModel.uiState.collectAsState()
    val timelines by timelineViewModel.timelines.collectAsState()
    val selectedDate by timelineViewModel.selectedDate.collectAsState()
    val displayDate = remember(selectedDate) {
        selectedDate.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
        )
    }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val screenState = uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = screenState.message,
                    onRetry = { timelineViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    if (timelines.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TimelineDateSelector(
                                displayDate = displayDate,
                                canMoveForward = selectedDate.isBefore(java.time.LocalDate.now()),
                                onPrevious = { timelineViewModel.shiftDate(-1) },
                                onToday = { timelineViewModel.selectToday() },
                                onNext = { timelineViewModel.shiftDate(1) }
                            )
                            EmptyState(
                                icon = RgIcon.Timeline,
                                title = stringResource(R.string.timeline_empty_title),
                                description = stringResource(R.string.timeline_empty_description),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onOpenDailyReport,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(stringResource(R.string.timeline_open_report))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(RgDimens.Space3),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                RgDimens.Space4
                            )
                        ) {
                            item {
                                Text(
                                    text = stringResource(R.string.timeline_title),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TimelineDateSelector(
                                    displayDate = displayDate,
                                    canMoveForward = selectedDate.isBefore(java.time.LocalDate.now()),
                                    onPrevious = { timelineViewModel.shiftDate(-1) },
                                    onToday = { timelineViewModel.selectToday() },
                                    onNext = { timelineViewModel.shiftDate(1) }
                                )
                                Spacer(modifier = Modifier.height(RgDimens.Space1))
                                Text(
                                    text = stringResource(
                                        R.string.timeline_summary,
                                        timelines.size,
                                        timelines.sumOf { it.events.size }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = onOpenDailyReport) {
                                    Text(stringResource(R.string.timeline_open_report))
                                }
                            }
                            items(timelines, key = { it.route.id }) { timeline ->
                                RouteTimelineItem(timeline)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineDateSelector(
    displayDate: String,
    canMoveForward: Boolean,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onPrevious) {
                Text(stringResource(R.string.timeline_previous_day))
            }
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.timeline_today))
            }
            TextButton(onClick = onNext, enabled = canMoveForward) {
                Text(stringResource(R.string.timeline_next_day))
            }
        }
        Text(
            text = displayDate,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RouteTimelineItem(timeline: RouteTimeline) {
    Column(verticalArrangement = Arrangement.spacedBy(RgDimens.Space2)) {
        Text(
            text = stringResource(
                R.string.tracker_route_summary,
                timeline.route.distanceMeters / 1000.0,
                timeline.route.durationSeconds / 60
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = RgDimens.Space4)
        )
        if (timeline.events.isEmpty()) {
            Text(
                text = stringResource(R.string.timeline_no_events),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = RgDimens.Space2)
            )
        } else {
            timeline.events.forEachIndexed { index, event ->
                TimelineEventItem(event, isLast = index == timeline.events.lastIndex)
            }
        }
    }
}

@Composable
private fun TimelineEventItem(event: TimelineEvent, isLast: Boolean) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeRange = "${timeFormatter.format(Date(event.startTime))}–${timeFormatter.format(Date(event.endTime))}"
    val durationMinutes = ((event.endTime - event.startTime).coerceAtLeast(0) / 60_000)
    val activityLabel = stringResource(
        when {
            event.isStay -> R.string.timeline_event_long_stop
            event.activityType == "STATIONARY" -> R.string.timeline_event_stop
            event.activityType == "WALKING" -> R.string.timeline_event_walking
            event.activityType == "RUNNING" -> R.string.timeline_event_running
            event.activityType == "CYCLING" -> R.string.timeline_event_cycling
            event.activityType == "PUBLIC_TRANSPORT" -> R.string.timeline_event_public_transport
            event.activityType == "IN_VEHICLE" -> R.string.timeline_event_vehicle
            else -> R.string.timeline_event_activity
        }
    )
    val details = buildList {
        add(stringResource(R.string.timeline_duration_minutes, durationMinutes))
        if (!event.isStay && event.distanceMeters > 0) {
            add(stringResource(R.string.timeline_distance_meters, event.distanceMeters.toInt()))
        }
    }.joinToString(" · ")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .padding(RgDimens.Space2)
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RgIcon.RgIconComposable(
                            icon = timelineEventIcon(event).copy(color = Color(0xFF317854)),
                            contentDescription = activityLabel
                        )
                    }
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RgDimens.Space4),
                    verticalArrangement = Arrangement.spacedBy(RgDimens.Space2)
                ) {
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activityLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .padding(start = 25.dp)
                    .width(2.dp)
                    .height(RgDimens.Space4)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }
}

private fun timelineEventIcon(event: TimelineEvent): RgIcon.IconData = when {
    event.isStay || event.activityType == "STATIONARY" -> RgIcon.Home
    event.activityType == "WALKING" || event.activityType == "RUNNING" -> RgIcon.Footprints
    event.activityType == "CYCLING" -> RgIcon.Bicycle
    event.activityType == "PUBLIC_TRANSPORT" -> RgIcon.Bus
    event.activityType == "IN_VEHICLE" -> RgIcon.Car
    else -> RgIcon.Route
}
