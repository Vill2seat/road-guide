package com.roadguide.app.feature.planner

import android.location.Geocoder
import android.util.Log
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.roadguide.app.R
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.data.source.OsrmRoutingSource
import com.roadguide.app.domain.model.RouteCategory
import com.roadguide.app.domain.model.RouteEvaluation
import com.roadguide.app.domain.model.RouteProfile
import com.roadguide.app.domain.usecase.impl.RouteComparisonEngine
import com.roadguide.app.domain.usecase.impl.RouteEvaluationEngine
import com.roadguide.app.map.MapView
import com.roadguide.app.map.addRouteEndpointMarker
import com.roadguide.app.map.addRoutePolyline
import com.roadguide.app.map.focusCameraOnPoints
import com.roadguide.app.map.model.MapPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PlannerScreen(
    modifier: Modifier = Modifier,
    osrmRoutingSource: OsrmRoutingSource? = null,
    routeEvaluationEngine: RouteEvaluationEngine? = null,
    routeComparisonEngine: RouteComparisonEngine? = null,
    locationSource: LocationSource? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val originFocusRequester = remember { FocusRequester() }
    val destinationFocusRequester = remember { FocusRequester() }

    // State for input fields (editable)
    val fromText = remember { mutableStateOf(context.getString(R.string.planner_current_location)) }
    val toText = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val routePoints = remember { mutableStateOf<List<MapPoint>?>(null) }
    val routeEvaluations = remember { mutableStateOf<List<RouteEvaluation>?>(null) }
    val selectedRoute = remember { mutableStateOf<RouteEvaluation?>(null) }
    val fromPointState = remember { mutableStateOf<MapPoint?>(null) }
    val toPointState = remember { mutableStateOf<MapPoint?>(null) }
    val routingError = remember { mutableStateOf<String?>(null) }
    val selectedProfile = remember { mutableStateOf(RouteProfile.DRIVING) }

    fun clearRouteResult() {
        routePoints.value = null
        routeEvaluations.value = null
        selectedRoute.value = null
        fromPointState.value = null
        toPointState.value = null
        routingError.value = null
    }

    @Suppress("DEPRECATION")
    suspend fun resolveAddress(address: String, isOrigin: Boolean): MapPoint {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) {
            throw PlannerInputException(
                context.getString(
                    if (isOrigin) R.string.planner_origin_required
                    else R.string.planner_destination_required
                )
            )
        }

        if (isOrigin && trimmed.equals(
                context.getString(R.string.planner_current_location),
                ignoreCase = true
            )
        ) {
            val source = locationSource
                ?: throw PlannerInputException(context.getString(R.string.planner_location_unavailable))
            if (!source.hasForegroundLocationPermission()) {
                throw PlannerInputException(context.getString(R.string.planner_location_unavailable))
            }
            val location = (try {
                source.getCurrentLocation()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                throw PlannerInputException(
                    context.getString(R.string.planner_location_unavailable)
                )
            }) ?: throw PlannerInputException(
                context.getString(R.string.planner_location_unavailable)
            )
            return MapPoint(location.latitude, location.longitude)
        }

        val coordinateParts = trimmed.split(',')
        if (coordinateParts.size == 2) {
            val latitude = coordinateParts[0].trim().toDoubleOrNull()
            val longitude = coordinateParts[1].trim().toDoubleOrNull()
            if (latitude != null && longitude != null) {
                if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
                    !longitude.isFinite() || longitude !in -180.0..180.0
                ) {
                    throw PlannerInputException(
                        context.getString(R.string.planner_invalid_coordinates)
                    )
                }
                return MapPoint(latitude, longitude)
            }
        }

        if (!Geocoder.isPresent()) {
            throw PlannerInputException(context.getString(R.string.planner_geocoder_unavailable))
        }
        val addresses = withContext(Dispatchers.IO) {
            Geocoder(context, Locale.getDefault()).getFromLocationName(trimmed, 1)
        }
        val match = addresses?.firstOrNull()
            ?: throw PlannerInputException(context.getString(R.string.planner_address_not_found))
        return MapPoint(match.latitude, match.longitude)
    }

    fun buildRoute() {
        if (isLoading.value) return

        isLoading.value = true
        clearRouteResult()

        coroutineScope.launch {
            try {
                val from = resolveAddress(fromText.value, isOrigin = true)
                val to = resolveAddress(toText.value, isOrigin = false)
                fromPointState.value = from
                toPointState.value = to

                val routingSource = osrmRoutingSource
                    ?: throw PlannerInputException(context.getString(R.string.planner_routing_unavailable))
                val evaluationEngine = routeEvaluationEngine
                    ?: throw PlannerInputException(context.getString(R.string.planner_routing_unavailable))
                val comparisonEngine = routeComparisonEngine
                    ?: throw PlannerInputException(context.getString(R.string.planner_routing_unavailable))
                val osrmRoutes = routingSource.route(
                    from,
                    to,
                    profile = selectedProfile.value
                )
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw PlannerInputException(
                        context.getString(R.string.planner_routing_unavailable)
                    )

                val evaluations = osrmRoutes.mapIndexed { index, osrmRoute ->
                    val category = when (index) {
                        0 -> RouteCategory.FASTEST
                        1 -> RouteCategory.LOWEST_EXPOSURE
                        else -> RouteCategory.BALANCED
                    }
                    evaluationEngine.evaluateRoute(
                        id = "route_$index",
                        geometry = osrmRoute.geometry,
                        durationSeconds = osrmRoute.durationSeconds,
                        category = category,
                        profile = selectedProfile.value
                    )
                }
                val classified = comparisonEngine.classifyRoutes(evaluations)
                val best = comparisonEngine.selectBestRoute(classified)
                    ?: throw PlannerInputException(
                        context.getString(R.string.planner_routing_unavailable)
                    )

                routeEvaluations.value = classified
                selectedRoute.value = best
                routePoints.value = best.geometry
            } catch (e: CancellationException) {
                throw e
            } catch (e: PlannerInputException) {
                routingError.value = e.message
            } catch (e: Exception) {
                Log.e("PlannerScreen", "Failed to build route", e)
                routingError.value = context.getString(R.string.planner_route_failed)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun selectRoute(evaluation: RouteEvaluation) {
        selectedRoute.value = evaluation
        routePoints.value = evaluation.geometry
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(RgDimens.Space2)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.planner_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.planner_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = RgDimens.Space4)
            )

            // Route input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // From
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.planner_from),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(RgDimens.Space2))
                        OutlinedTextField(
                            value = fromText.value,
                            onValueChange = {
                                fromText.value = it
                                clearRouteResult()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(originFocusRequester),
                            enabled = !isLoading.value,
                            label = { Text(stringResource(R.string.planner_origin_example)) },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.planner_current_location),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { destinationFocusRequester.requestFocus() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgColor.Primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(RgDimens.Space3))

                    // To
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.planner_to),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(RgDimens.Space2))
                        OutlinedTextField(
                            value = toText.value,
                            onValueChange = {
                                toText.value = it
                                clearRouteResult()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(destinationFocusRequester),
                            enabled = !isLoading.value,
                            label = { Text(stringResource(R.string.planner_destination)) },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.planner_destination),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { buildRoute() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgColor.Primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    Text(
                        text = stringResource(R.string.planner_travel_mode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(RgDimens.Space1)
                    ) {
                        RouteProfile.entries.forEach { profile ->
                            FilterChip(
                                selected = selectedProfile.value == profile,
                                enabled = !isLoading.value,
                                onClick = {
                                    if (selectedProfile.value != profile) {
                                        selectedProfile.value = profile
                                        clearRouteResult()
                                    }
                                },
                                label = {
                                    Text(
                                        stringResource(
                                            when (profile) {
                                                RouteProfile.DRIVING -> R.string.planner_mode_driving
                                                RouteProfile.WALKING -> R.string.planner_mode_walking
                                                RouteProfile.CYCLING -> R.string.planner_mode_cycling
                                            }
                                        )
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(RgDimens.Space4))

                    // Build route button
                    Button(
                        onClick = { buildRoute() },
                        enabled = !isLoading.value,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary)
                    ) {
                        Text(
                            text = if (isLoading.value) {
                                stringResource(R.string.planner_route_building)
                            } else {
                                stringResource(R.string.planner_build_route)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    if (isLoading.value) {
                        Spacer(modifier = Modifier.height(RgDimens.Space2))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    routingError.value?.let { error ->
                        Spacer(modifier = Modifier.height(RgDimens.Space2))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RgDimens.Space4))

            // Route comparison cards (multiple routes)
            routeEvaluations.value?.let { evaluations ->
                if (evaluations.size > 1) {
                    Text(
                        text = stringResource(R.string.planner_available_routes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = RgDimens.Space3)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        evaluations.forEach { eval ->
                            val isSelected = selectedRoute.value?.id == eval.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = RgDimens.Space2),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                onClick = { selectRoute(eval) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when (eval.category) {
                                                RouteCategory.FASTEST -> stringResource(R.string.planner_fastest_route)
                                                RouteCategory.LOWEST_EXPOSURE -> stringResource(R.string.planner_lowest_exposure_route)
                                                RouteCategory.BALANCED -> stringResource(R.string.planner_balanced_route)
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (eval.hasAirQualityData) {
                                                stringResource(
                                                    R.string.planner_route_data_summary,
                                                    eval.coverage.toInt(),
                                                    (eval.confidence * 100).roundToInt()
                                                )
                                            } else {
                                                stringResource(R.string.air_quality_insufficient)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (eval.hasAirQualityData) {
                                                eval.exposureScore.roundToInt().toString()
                                            } else {
                                                "—"
                                            },
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (!eval.hasAirQualityData) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else when {
                                                eval.exposureScore < 30 -> RgColor.ExposureGood
                                                eval.exposureScore < 60 -> RgColor.ExposureModerate
                                                else -> RgColor.ExposureHigh
                                            }
                                        )
                                        Text(
                                            text = if (eval.hasAirQualityData) {
                                                stringResource(R.string.planner_exposure_label)
                                            } else {
                                                stringResource(R.string.exposure_unavailable)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                }
            }

            // Route result card
            if (routePoints.value != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.planner_route_built,
                                routePoints.value?.size ?: 0
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space3))

                        // Route summary (visual route rendered on map)
                        MapView(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            mapContentKey = routePoints.value,
                            onMapUpdate = { mapLibreMap ->
                                val ctx = context
                                // Add location markers for from/to points
                                fromPointState.value?.let { fromPt ->
                                    mapLibreMap.addRouteEndpointMarker(
                                        ctx,
                                        "route_origin",
                                        fromPt.latitude,
                                        fromPt.longitude
                                    )
                                }
                                toPointState.value?.let { toPt ->
                                    mapLibreMap.addRouteEndpointMarker(
                                        ctx,
                                        "route_destination",
                                        toPt.latitude,
                                        toPt.longitude
                                    )
                                }
                                // Add route polyline
                                val routePts = routePoints.value
                                routePts?.let { points ->
                                    mapLibreMap.addRoutePolyline(points)
                                    mapLibreMap.focusCameraOnPoints(points)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(RgDimens.Space4))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val selected = selectedRoute.value
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.planner_distance),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val distKm = (selected?.distanceMeters ?: 0.0) / 1000.0
                                Text(
                                    text = stringResource(R.string.planner_distance_km, distKm),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.planner_exposure_estimate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val exposure = selected
                                    ?.takeIf { it.hasAirQualityData }
                                    ?.exposureScore
                                val exposureLabel = exposure?.let {
                                    when {
                                    it < 30 -> stringResource(R.string.planner_exposure_low)
                                    it < 60 -> stringResource(R.string.planner_exposure_moderate)
                                    else -> stringResource(R.string.planner_exposure_high)
                                    }
                                } ?: stringResource(R.string.air_quality_insufficient)
                                Text(
                                    text = exposureLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = exposure?.let {
                                    when {
                                        it < 30 -> RgColor.ExposureGood
                                        it < 60 -> RgColor.ExposureModerate
                                        else -> RgColor.ExposureHigh
                                    }
                                    } ?: MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Eco route warning from comparison engine
                        routeEvaluations.value?.let { evaluations ->
                            routeComparisonEngine?.let { comparison ->
                                comparison.getEcoRouteWarning(evaluations)?.let {
                                    Spacer(modifier = Modifier.height(RgDimens.Space3))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Text(
                                            text = stringResource(R.string.planner_insufficient_data_warning),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }

                            }
                        }
                    }
                }
            } else {
                // Show placeholder when no route is built
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.planner_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private class PlannerInputException(message: String) : Exception(message)
