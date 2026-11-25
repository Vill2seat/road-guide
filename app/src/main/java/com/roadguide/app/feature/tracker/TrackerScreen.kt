package com.roadguide.app.feature.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
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
    onOpenDailyReport: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
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

    Box(modifier = modifier.fillMaxSize()) {
        val screenState = uiState
        when (screenState) {
            is ScreenState.Loading -> {
                LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            }
            is ScreenState.Error -> {
                ErrorPlaceholder(
                    message = screenState.message,
                    onRetry = { trackerViewModel.loadTrackingState() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            ScreenState.Success, ScreenState.Empty -> {
                // Full-screen map
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

                // Top overlay: location pill (left) + profile button (right)
                TrackerTopOverlay(
                    locationLabel = location?.let { loc ->
                        String.format(Locale.getDefault(), "%.4f, %.4f", loc.latitude, loc.longitude)
                    } ?: stringResource(R.string.tracker_current_location),
                    trackingEnabled = trackingEnabled,
                    onProfileClick = onOpenProfile
                )

                // Right-side map controls
                TrackerMapControls(
                    onLocate = {
                        location?.let { loc ->
                            // Camera focus handled by map content key change
                        }
                    },
                    onLayers = { },
                    onCompass = { }
                )

                // Bottom sheet with cards
                TrackerBottomSheet(
                    trackingEnabled = trackingEnabled,
                    isPaused = isPaused,
                    onToggleTracking = { trackerViewModel.toggleTracking(it) },
                    onPauseResume = {
                        if (isPaused) trackerViewModel.resumeTracking()
                        else trackerViewModel.pauseTracking()
                    },
                    onStop = { trackerViewModel.stopTracking() },
                    todayRoute = todayRoute,
                    pointCount = pointCount,
                    environmentalSample = environmentalSample,
                    dailyReport = dailyReport,
                    onOpenTimeline = onOpenTimeline,
                    onOpenDailyReport = onOpenDailyReport
                )
            }
        }
    }
}

@Composable
private fun TrackerTopOverlay(
    locationLabel: String,
    trackingEnabled: Boolean,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 48.dp,
                start = RgDimens.Space4,
                end = RgDimens.Space4
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(RgDimens.LocationPillRadius))
                .background(RgColor.PrimaryContainer)
                .padding(horizontal = RgDimens.Space3, vertical = RgDimens.Space2)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RgDimens.Space1)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = RgColor.Primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (trackingEnabled) locationLabel else stringResource(R.string.tracker_state_stopped),
                    style = MaterialTheme.typography.labelMedium,
                    color = RgColor.Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Profile button
        Box(
            modifier = Modifier
                .size(RgDimens.MapControlSize)
                .shadow(RgDimens.Elevation2, RoundedCornerShape(RgDimens.MapControlRadius))
                .clip(RoundedCornerShape(RgDimens.MapControlRadius))
                .background(RgColor.Surface)
        ) {
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.nav_profile),
                    tint = RgColor.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackerMapControls(
    onLocate: () -> Unit,
    onLayers: () -> Unit,
    onCompass: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = RgDimens.Space4, top = 120.dp),
        verticalArrangement = Arrangement.spacedBy(RgDimens.Space3)
    ) {
        MapControlButton(
            icon = Icons.Default.MyLocation,
            contentDescription = "Locate me",
            onClick = onLocate
        )
        MapControlButton(
            icon = Icons.Default.Layers,
            contentDescription = "Map layers",
            onClick = onLayers
        )
        MapControlButton(
            icon = Icons.Default.Explore,
            contentDescription = "Compass",
            onClick = onCompass
        )
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(RgDimens.MapControlSize)
            .shadow(RgDimens.Elevation2, RoundedCornerShape(RgDimens.MapControlRadius))
            .clip(RoundedCornerShape(RgDimens.MapControlRadius))
            .background(RgColor.Surface)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = RgColor.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TrackerBottomSheet(
    trackingEnabled: Boolean,
    isPaused: Boolean,
    onToggleTracking: (Boolean) -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    todayRoute: com.roadguide.app.data.local.entity.Route?,
    pointCount: Int,
    environmentalSample: com.roadguide.app.data.local.entity.EnvironmentalSample?,
    dailyReport: com.roadguide.app.data.local.entity.DailyReport?,
    onOpenTimeline: () -> Unit,
    onOpenDailyReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = RgDimens.RadiusSheet, topEnd = RgDimens.RadiusSheet))
            .background(RgColor.Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .padding(vertical = RgDimens.Space2)
                .align(Alignment.CenterHorizontally)
                .width(RgDimens.DragHandleWidth)
                .height(RgDimens.DragHandleHeight)
                .clip(RoundedCornerShape(RgDimens.DragHandleHeight))
                .background(RgColor.DragHandle)
        )

        Column(
            modifier = Modifier.padding(horizontal = RgDimens.Space4),
            verticalArrangement = Arrangement.spacedBy(RgDimens.Space3)
        ) {
            // Today's Overview card
            TrackerCard(
                title = stringResource(R.string.tracker_today_route)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = trackingEnabled,
                        onCheckedChange = onToggleTracking,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RgColor.Primary,
                            checkedTrackColor = RgColor.PrimaryContainer,
                            uncheckedThumbColor = RgColor.OnSurfaceVariant,
                            uncheckedTrackColor = RgColor.SurfaceVariant
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
                        style = MaterialTheme.typography.labelMedium,
                        color = if (trackingEnabled) RgColor.Success else RgColor.OnSurfaceVariant
                    )
                }

                if (trackingEnabled) {
                    Spacer(modifier = Modifier.height(RgDimens.Space2))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RgDimens.Space2)
                    ) {
                        Button(
                            onClick = onPauseResume,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RgColor.Primary,
                                contentColor = RgColor.OnPrimary
                            ),
                            shape = RoundedCornerShape(RgDimens.RadiusMedium)
                        ) {
                            Text(
                                stringResource(
                                    if (isPaused) R.string.tracker_resume else R.string.tracker_pause
                                )
                            )
                        }
                        Button(
                            onClick = onStop,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RgColor.Error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(RgDimens.RadiusMedium)
                        ) {
                            Text(stringResource(R.string.tracker_stop))
                        }
                    }
                }

                todayRoute?.let { route ->
                    Spacer(modifier = Modifier.height(RgDimens.Space2))
                    Text(
                        text = stringResource(
                            R.string.tracker_route_summary,
                            route.distanceMeters / 1000.0,
                            route.totalDurationSeconds / 60
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = RgColor.OnSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.tracker_route_points, pointCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = RgColor.OnSurfaceVariant
                )
            }

            // Daily Exposure card
            TrackerCard(
                title = stringResource(R.string.daily_report_title)
            ) {
                val report = dailyReport
                if (report != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${report.totalScore.toInt()}",
                            style = MaterialTheme.typography.displaySmall,
                            color = RgColor.Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "/ 100",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RgColor.OnSurfaceVariant
                            )
                            Text(
                                text = stringResource(
                                    R.string.tracker_report_summary,
                                    report.totalScore.toInt(),
                                    report.confidence.toInt(),
                                    report.coverage.toInt()
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = RgColor.OnSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.tracker_report_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = RgColor.OnSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onOpenTimeline) {
                        Text(
                            stringResource(R.string.tracker_open_timeline),
                            color = RgColor.Primary
                        )
                    }
                    TextButton(onClick = onOpenDailyReport) {
                        Text(
                            stringResource(R.string.tracker_open_report),
                            color = RgColor.Primary
                        )
                    }
                }
            }

            // Current Environment card
            TrackerCard(
                title = stringResource(R.string.tracker_environment_title)
            ) {
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
                    } ?: RgColor.OnSurfaceVariant
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = aqiColor,
                            fontWeight = FontWeight.Medium
                        )
                        sample.temperatureCelsius?.let { temperature ->
                            Text(
                                text = stringResource(
                                    R.string.tracker_temperature_value,
                                    temperature
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = RgColor.OnSurfaceVariant
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
                        color = RgColor.OnSurfaceVariant
                    )
                } ?: Text(
                    text = stringResource(R.string.air_quality_insufficient),
                    style = MaterialTheme.typography.bodySmall,
                    color = RgColor.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrackerCard(
    title: String,
    content: @Composable Column.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RgDimens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = RgColor.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = RgDimens.Elevation1)
    ) {
        Column(
            modifier = Modifier.padding(RgDimens.Space4)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = RgColor.OnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(RgDimens.Space2))
            content()
        }
    }
}
