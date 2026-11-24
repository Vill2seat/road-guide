package com.roadguide.app.feature.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.R
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.feature.tracker.viewmodel.TrackerViewModel
import com.roadguide.app.map.MapView
import com.roadguide.app.map.addLocationMarker
import com.roadguide.app.map.addRoutePolylines
import com.roadguide.app.map.focusCameraOnPoints
import com.roadguide.app.map.model.MapPoint
import com.roadguide.app.map.removeLocationMarker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackerScreen(
    modifier: Modifier = Modifier,
    onOpenTimeline: () -> Unit = {},
    onOpenDailyReport: () -> Unit = {}
) {
    val trackerViewModel: TrackerViewModel = hiltViewModel()
    val context = LocalContext.current

    val uiState by trackerViewModel.uiState.collectAsState()
    val trackingEnabled by trackerViewModel.trackingEnabled.collectAsState()
    val isPaused by trackerViewModel.isPaused.collectAsState()
    val currentRoutePoints by trackerViewModel.currentRoutePoints.collectAsState()
    val routeLineChunks by trackerViewModel.routeLineChunks.collectAsState()
    val todayRoute by trackerViewModel.todayRoute.collectAsState()
    val environmentalSample by trackerViewModel.environmentalSample.collectAsState()
    val dailyReport by trackerViewModel.dailyReport.collectAsState()
    val location by trackerViewModel.locationUpdates.collectAsState()

    val pointCount = currentRoutePoints.size
    val mapContentKey = Triple(
        todayRoute?.id,
        routeLineChunks,
        location?.time
    )

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val screenState = uiState
            when (screenState) {
                is com.roadguide.app.core.common.ScreenState.Loading -> {
                    LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                }
                is com.roadguide.app.core.common.ScreenState.Error -> {
                    ErrorPlaceholder(
                        message = screenState.message,
                        onRetry = { trackerViewModel.loadTrackingState() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                com.roadguide.app.core.common.ScreenState.Success,
                com.roadguide.app.core.common.ScreenState.Empty -> {
                    // Full-screen map with live location marker
                    MapView(
                        modifier = Modifier.fillMaxSize(),
                        mapContentKey = mapContentKey,
                        onMapUpdate = { mapLibreMap ->
                            location?.let { loc ->
                                mapLibreMap.addLocationMarker(context, loc.latitude, loc.longitude)
                            } ?: mapLibreMap.removeLocationMarker()
                            mapLibreMap.addRoutePolylines(routeLineChunks)
                        },
                        onMapReady = { mapLibreMap ->
                            location?.let { loc ->
                                mapLibreMap.addLocationMarker(context, loc.latitude, loc.longitude)
                            } ?: mapLibreMap.removeLocationMarker()
                            val routePoints = routeLineChunks.flatten()
                            if (routePoints.isNotEmpty()) {
                                mapLibreMap.focusCameraOnPoints(routePoints)
                            } else {
                                location?.let { loc ->
                                    mapLibreMap.focusCameraOnPoints(
                                        listOf(MapPoint(loc.latitude, loc.longitude))
                                    )
                                }
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                top = RgDimens.Space4,
                                bottom = RgDimens.Space4,
                                start = RgDimens.Space3,
                                end = RgDimens.Space3
                            )
                    ) {
                        Text(
                            text = stringResource(R.string.screen_tracker),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(RgDimens.Space3))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Tracking switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Switch(
                                        checked = trackingEnabled,
                                        onCheckedChange = { trackerViewModel.toggleTracking(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                    Text(
                                        text = stringResource(
                                            when {
                                                !trackingEnabled -> R.string.tracker_state_stopped
                                                isPaused -> R.string.tracker_state_paused
                                                else -> R.string.tracker_state_recording
                                            }
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (trackingEnabled) RgColor.Success else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (trackingEnabled) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = RgDimens.Space2),
                                        horizontalArrangement = Arrangement.spacedBy(RgDimens.Space2)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (isPaused) trackerViewModel.resumeTracking()
                                                else trackerViewModel.pauseTracking()
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                stringResource(
                                                    if (isPaused) R.string.tracker_resume
                                                    else R.string.tracker_pause
                                                )
                                            )
                                        }
                                        Button(
                                            onClick = { trackerViewModel.stopTracking() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text(stringResource(R.string.tracker_stop))
                                        }
                                    }
                                }

                                todayRoute?.let { route ->
                                    Spacer(modifier = Modifier.height(RgDimens.Space3))
                                    Text(
                                        text = stringResource(R.string.tracker_today_route),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.tracker_route_summary,
                                            route.distanceMeters / 1000.0,
                                            route.totalDurationSeconds / 60
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = RgDimens.Space3),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.tracker_route_points, pointCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(RgDimens.Space2))
                                Text(
                                    text = stringResource(R.string.tracker_environment_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                environmentalSample?.let { sample ->
                                    val aqi = sample.europeanAqi?.takeIf { it in 0..500 }
                                        ?: sample.usAqi?.takeIf { it in 0..500 }
                                    val aqiColor = aqi?.let {
                                        when (it) {
                                            in 0..50 -> RgColor.ExposureGood
                                            in 51..100 -> RgColor.ExposureModerate
                                            in 101..150 -> RgColor.ExposureHigh
                                            else -> RgColor.ExposureVeryHigh
                                        }
                                    } ?: MaterialTheme.colorScheme.onSurfaceVariant
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (aqi != null) {
                                                "${stringResource(R.string.aqi)}$aqi"
                                            } else {
                                                stringResource(R.string.air_quality_insufficient)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = aqiColor
                                        )
                                        sample.temperatureCelsius?.let { temperature ->
                                            Text(
                                                text = stringResource(
                                                    R.string.tracker_temperature_value,
                                                    temperature
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(
                                            R.string.tracker_environment_sample_time,
                                            SimpleDateFormat("HH:mm", Locale.getDefault())
                                                .format(Date(sample.timestamp))
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } ?: Text(
                                    text = stringResource(R.string.air_quality_insufficient),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(RgDimens.Space3))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(
                                            text = stringResource(R.string.daily_report_title),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        val report = dailyReport
                                        if (report != null) {
                                            Text(
                                                text = stringResource(
                                                    R.string.tracker_report_summary,
                                                    report.totalScore.toInt(),
                                                    report.confidence.toInt(),
                                                    report.coverage.toInt()
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        } else {
                                            Text(
                                                text = stringResource(R.string.tracker_report_unavailable),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            TextButton(onClick = onOpenTimeline) {
                                                Text(stringResource(R.string.tracker_open_timeline))
                                            }
                                            TextButton(onClick = onOpenDailyReport) {
                                                Text(stringResource(R.string.tracker_open_report))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(RgDimens.Space4))

                        // Current location info
                        location?.let { loc ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = stringResource(R.string.tracker_current_location),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = String.format(
                                                Locale.getDefault(),
                                                "%.5f, %.5f",
                                                loc.latitude,
                                                loc.longitude
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = stringResource(R.string.tracker_speed_value, loc.speed),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.tracker_accuracy_value, loc.accuracy),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
