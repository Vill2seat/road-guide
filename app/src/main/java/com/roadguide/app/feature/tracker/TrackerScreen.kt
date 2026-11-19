package com.roadguide.app.feature.tracker

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.feature.tracker.viewmodel.RecordingState
import com.roadguide.app.feature.tracker.viewmodel.TrackerViewModel
import com.roadguide.app.map.MapView
import com.roadguide.app.map.addLocationMarker
import com.roadguide.app.map.addRoutePolyline
import com.roadguide.app.map.focusCameraOnPoints
import com.roadguide.app.map.model.MapPoint

@Composable
fun TrackerScreen(
    modifier: Modifier = Modifier
) {
    val trackerViewModel: TrackerViewModel = hiltViewModel()
    val context = LocalContext.current

    val uiState by trackerViewModel.uiState.collectAsState()
    val trackingEnabled by trackerViewModel.trackingEnabled.collectAsState()
    val recordingState by trackerViewModel.recordingState.collectAsState()
    val currentRoutePoints by trackerViewModel.currentRoutePoints.collectAsState()
    val environmentalSample by trackerViewModel.environmentalSample.collectAsState()
    val location by trackerViewModel.locationUpdates.collectAsState()

    val pointCount = currentRoutePoints.size

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
                        onMapReady = { mapLibreMap ->
                            val ctx = context
                            location?.let { loc ->
                                mapLibreMap.addLocationMarker(ctx, loc.latitude, loc.longitude)
                            }
                            val routePoints = currentRoutePoints.map { p ->
                                MapPoint(latitude = p.latitude, longitude = p.longitude)
                            }
                            if (routePoints.isNotEmpty()) {
                                mapLibreMap.addRoutePolyline(routePoints)
                                mapLibreMap.focusCameraOnPoints(routePoints)
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = RgDimens.Space4,
                                bottom = RgDimens.Space4,
                                start = RgDimens.Space3,
                                end = RgDimens.Space3
                            )
                    ) {
                        Text(
                            text = "Улицы.ру — городской сервис",
                            style = MaterialTheme.typography.bodySmall,
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
                                        text = if (trackingEnabled) "ВКЛ" else "ВЫКЛ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (trackingEnabled) RgColor.Success else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = RgDimens.Space3),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Точек: $pointCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Environmental sample info
                                environmentalSample?.let { sample ->
                                    val aqi = sample.europeanAqi ?: sample.usAqi ?: 0
                                    val aqiColor = when (aqi) {
                                        in 0..50 -> RgColor.ExposureGood
                                        in 51..100 -> RgColor.ExposureModerate
                                        in 101..150 -> RgColor.ExposureHigh
                                        else -> RgColor.ExposureVeryHigh
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "AQI: $aqi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = aqiColor
                                        )
                                        Text(
                                            text = "Погода: ${sample.temperatureCelsius?.toInt()}°C",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                                            text = "Текущее положение",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${String.format("%.5f", loc.latitude)}, ${String.format("%.5f", loc.longitude)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "${loc.speed} м/с",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Точность ${loc.accuracy} м",
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
