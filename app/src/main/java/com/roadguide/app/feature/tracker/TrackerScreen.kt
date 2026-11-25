package com.roadguide.app.feature.tracker
import org.maplibre.android.maps.MapLibreMap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.roadguide.app.core.ui.theme.RgIcon
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    modifier: Modifier = Modifier,
    onOpenTimeline: () -> Unit = {},
    onOpenDailyReport: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenMapSettings: () -> Unit = {}
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
    val mapReference = remember { mutableStateOf<MapLibreMap?>(null) }
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val mapContentKey = Triple(todayRoute?.id, routeLineChunks, location?.time)

    Scaffold(modifier = modifier.fillMaxSize()) { outerPadding ->
        when (val state = uiState) {
            ScreenState.Loading -> LoadingPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding)
            )
            is ScreenState.Error -> ErrorPlaceholder(
                message = state.message,
                onRetry = { trackerViewModel.loadTrackingState() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding)
            )
            ScreenState.Success, ScreenState.Empty -> {
                BottomSheetScaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(outerPadding),
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 176.dp,
                    sheetShape = RoundedCornerShape(
                        topStart = RgDimens.RadiusLarge,
                        topEnd = RgDimens.RadiusLarge
                    ),
                    sheetContainerColor = MaterialTheme.colorScheme.background,
                    sheetContent = {
                        TrackerBottomSheet(
                            trackingEnabled = trackingEnabled,
                            isPaused = isPaused,
                            onToggleTracking = trackerViewModel::toggleTracking,
                            onPauseResume = {
                                if (isPaused) trackerViewModel.resumeTracking()
                                else trackerViewModel.pauseTracking()
                            },
                            onStop = trackerViewModel::stopTracking,
                            todayRoute = todayRoute,
                            pointCount = pointCount,
                            environmentalSample = environmentalSample,
                            dailyReport = dailyReport,
                            onOpenTimeline = onOpenTimeline,
                            onOpenDailyReport = onOpenDailyReport
                        )
                    },
                    sheetDragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
                ) {
                    Box(Modifier.fillMaxSize()) {
                        MapView(
                            modifier = Modifier.fillMaxSize(),
                            mapContentKey = mapContentKey,
                            onMapUpdate = { map ->
                                mapReference.value = map
                                location?.let { map.addLocationMarker(context, it.latitude, it.longitude) }
                                    ?: map.removeLocationMarker()
                                map.addRoutePolylines(routeLineChunks)
                            },
                            onMapReady = { map ->
                                mapReference.value = map
                                val points = routeLineChunks.flatten()
                                if (points.isNotEmpty()) {
                                    map.focusCameraOnPoints(points)
                                } else {
                                    location?.let {
                                        map.focusCameraOnPoints(
                                            listOf(MapPoint(it.latitude, it.longitude))
                                        )
                                    }
                                }
                            }
                        )
                        TrackingStatusPill(
                            enabled = trackingEnabled,
                            paused = isPaused,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(start = RgDimens.Space4, top = RgDimens.Space2)
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(end = RgDimens.Space4, top = RgDimens.Space2),
                            verticalArrangement = Arrangement.spacedBy(RgDimens.Space2)
                        ) {
                            MapActionButton(
                                icon = RgIcon.User,
                                description = stringResource(R.string.nav_profile),
                                onClick = onOpenProfile
                            )
                            MapActionButton(
                                icon = RgIcon.Layers,
                                description = stringResource(R.string.settings_map),
                                onClick = onOpenMapSettings
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                location?.let {
                                    mapReference.value?.focusCameraOnPoints(
                                        listOf(MapPoint(it.latitude, it.longitude))
                                    )
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = RgDimens.Space4, bottom = 194.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = Color(0xFF317854),
                            shape = CircleShape
                        ) {
                            RgIcon.RgIconComposable(
                                icon = RgIcon.Locate,
                                contentDescription = stringResource(R.string.tracker_locate_me)
                            )
                        }
                    }
                }
            }
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
            .fillMaxWidth()
            .heightIn(max = 720.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RgDimens.Space4)
            .padding(bottom = RgDimens.Space6),
        verticalArrangement = Arrangement.spacedBy(RgDimens.Space3)
    ) {
        Text(
            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.tracker_overview_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(RgDimens.Space4),
                verticalArrangement = Arrangement.spacedBy(RgDimens.Space3)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OverviewMetric(
                        value = stringResource(
                            R.string.daily_report_distance_km,
                            (todayRoute?.distanceMeters ?: 0.0) / 1000.0
                        ),
                        label = stringResource(R.string.daily_report_distance)
                    )
                    OverviewMetric(
                        value = pointCount.toString(),
                        label = stringResource(R.string.tracker_points_label)
                    )
                    OverviewMetric(
                        value = dailyReport?.visitedPlacesCount?.toString() ?: "—",
                        label = stringResource(R.string.daily_report_places)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RgIcon.RgIconComposable(
                        icon = RgIcon.Gauge.copy(color = RgColor.ExposureModerate),
                        modifier = Modifier.size(RgDimens.IconLarge)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = RgDimens.Space3)
                    ) {
                        Text(
                            stringResource(R.string.daily_report_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            dailyReport?.let {
                                stringResource(
                                    R.string.tracker_report_summary,
                                    it.totalScore.toInt(),
                                    it.confidence.toInt(),
                                    it.coverage.toInt()
                                )
                            } ?: stringResource(R.string.tracker_report_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenDailyReport) {
                        Text(stringResource(R.string.common_view))
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(RgDimens.Space4),
                verticalArrangement = Arrangement.spacedBy(RgDimens.Space2)
            ) {
                Text(
                    stringResource(R.string.tracker_environment_title),
                    style = MaterialTheme.typography.titleMedium
                )
                environmentalSample?.let { sample ->
                    val aqi = sample.europeanAqi?.takeIf { it in 0..500 }
                        ?: sample.usAqi?.takeIf { it in 0..500 }
                    Text(
                        text = aqi?.let { "${stringResource(R.string.aqi)}$it" }
                            ?: stringResource(R.string.air_quality_insufficient),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    sample.temperatureCelsius?.let { temperature ->
                        Text(
                            text = stringResource(R.string.tracker_temperature_value, temperature),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        stringResource(
                            R.string.tracker_environment_sample_time,
                            SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(Date(sample.timestamp))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } ?: Text(
                    stringResource(R.string.air_quality_insufficient),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trackingEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RgDimens.Space2)
            ) {
                Button(
                    onClick = onPauseResume,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = Color(0xFF317854)
                    )
                ) {
                    Text(
                        stringResource(
                            if (isPaused) R.string.tracker_resume else R.string.tracker_pause
                        )
                    )
                }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.tracker_stop))
                }
            }
        } else {
            Button(
                onClick = { onToggleTracking(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF317854))
            ) {
                Text(stringResource(R.string.tracker_start))
            }
        }
        OutlinedButton(
            onClick = onOpenTimeline,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.tracker_open_timeline))
        }
    }
}

@Composable
private fun OverviewMetric(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrackingStatusPill(enabled: Boolean, paused: Boolean, modifier: Modifier = Modifier) {
    val label = when {
        !enabled -> stringResource(R.string.tracker_state_stopped)
        paused -> stringResource(R.string.tracker_state_paused)
        else -> stringResource(R.string.tracker_state_recording)
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(RgDimens.RadiusFull),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RgDimens.Space4, vertical = RgDimens.Space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RgIcon.RgIconComposable(
                icon = RgIcon.CheckCircle.copy(
                    color = if (enabled && !paused) Color(0xFF317854) else Color(0xFF596B61)
                ),
                modifier = Modifier.size(16.dp),
                contentDescription = null
            )
            Spacer(Modifier.width(RgDimens.Space2))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MapActionButton(
    icon: RgIcon.IconData,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .then(Modifier),
    ) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = RgDimens.Elevation2)
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                RgIcon.RgIconComposable(
                    icon = icon.copy(color = Color(0xFF317854)),
                    contentDescription = description
                )
            }
        }
    }
}
