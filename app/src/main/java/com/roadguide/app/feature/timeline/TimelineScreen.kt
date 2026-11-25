package com.roadguide.app.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.components.EmptyState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.domain.model.RouteTimeline
import com.roadguide.app.domain.model.TimelineEvent
import com.roadguide.app.feature.timeline.viewmodel.TimelineViewModel
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    onOpenDailyReport: () -> Unit = {}
) {
    val timelineViewModel: TimelineViewModel = hiltViewModel()
    val uiState by timelineViewModel.uiState.collectAsState()
    val routes by timelineViewModel.timelines.collectAsState()
    val selectedDate by timelineViewModel.selectedDate.collectAsState()
    val displayDate = remember(selectedDate) {
        selectedDate.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RgDimens.Space4, vertical = RgDimens.Space2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { timelineViewModel.shiftDate(-1) }) {
                Text(stringResource(R.string.timeline_previous_day))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(displayDate, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = timelineViewModel::selectToday) {
                    Text(stringResource(R.string.timeline_today))
                }
            }
            TextButton(
                onClick = { timelineViewModel.shiftDate(1) },
                enabled = selectedDate.isBefore(java.time.LocalDate.now())
            ) {
                Text(stringResource(R.string.timeline_next_day))
            }
        }
        TextButton(
            onClick = onOpenDailyReport,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.timeline_open_report))
        }
        when (val state = uiState) {
            ScreenState.Loading -> LoadingPlaceholder(Modifier.fillMaxSize())
            is ScreenState.Error -> ErrorPlaceholder(
                message = state.message,
                onRetry = timelineViewModel::loadData,
                modifier = Modifier.fillMaxSize()
            )
            ScreenState.Success, ScreenState.Empty -> {
                if (routes.isEmpty()) {
                    EmptyState(
                        icon = RgIcon.Timeline,
                        title = stringResource(R.string.timeline_empty_title),
                        description = stringResource(R.string.timeline_empty_description),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    TimelineList(routes = routes, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TimelineList(routes: List<RouteTimeline>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = RgDimens.Space4,
            vertical = RgDimens.Space4
        ),
        verticalArrangement = Arrangement.spacedBy(RgDimens.Space3)
    ) {
        items(routes, key = { it.route.id }) { route ->
            RouteTimelineItem(route)
        }
    }
}

@Composable
private fun RouteTimelineItem(timeline: RouteTimeline) {
    Column(verticalArrangement = Arrangement.spacedBy(RgDimens.Space2)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RgDimens.RadiusCard),
            colors = CardDefaults.cardColors(containerColor = RgColor.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = RgDimens.Elevation1)
        ) {
            Column(
                modifier = Modifier.padding(RgDimens.Space4),
                verticalArrangement = Arrangement.spacedBy(RgDimens.Space2)
            ) {
                Text(
                    text = stringResource(
                        R.string.tracker_route_summary,
                        timeline.route.distanceMeters / 1000.0,
                        timeline.route.durationSeconds / 60
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (timeline.events.isEmpty()) {
                    Text(
                        text = stringResource(R.string.timeline_no_events),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    timeline.events.forEachIndexed { index, event ->
                        TimelineEventItem(event, isLast = index == timeline.events.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEventItem(event: TimelineEvent, isLast: Boolean) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeRange =
        "${timeFormatter.format(Date(event.startTime))}–${timeFormatter.format(Date(event.endTime))}"
    val durationMinutes = (event.endTime - event.startTime).coerceAtLeast(0) / 60_000
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

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.width(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                RgIcon.RgIconComposable(
                    icon = timelineEventIcon(event).copy(color = RgColor.Primary),
                    contentDescription = activityLabel
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(RgDimens.Space4)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(timeRange, style = MaterialTheme.typography.labelMedium)
            Text(activityLabel, style = MaterialTheme.typography.titleSmall)
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
