package com.roadguide.app.feature.planner

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roadguide.app.R
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.data.source.OsrmRoutingSource
import com.roadguide.app.domain.model.RouteCategory
import com.roadguide.app.domain.model.RouteEvaluation
import com.roadguide.app.domain.model.RouteProfile
import com.roadguide.app.domain.usecase.impl.RouteComparisonEngine
import com.roadguide.app.domain.usecase.impl.RouteEvaluationEngine
import com.roadguide.app.map.MapView
import com.roadguide.app.map.addLocationMarker
import com.roadguide.app.map.addRoutePolyline
import com.roadguide.app.map.focusCameraOnPoints
import com.roadguide.app.map.model.MapPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PlannerScreen(
    modifier: Modifier = Modifier,
    osrmRoutingSource: OsrmRoutingSource? = null,
    routeEvaluationEngine: RouteEvaluationEngine? = null,
    routeComparisonEngine: RouteComparisonEngine? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State for input fields (editable)
    val fromText = remember { mutableStateOf("Текущее положение") }
    val toText = remember { mutableStateOf("Адрес назначения") }
    val isLoading = remember { mutableStateOf(false) }
    val routePoints = remember { mutableStateOf<List<MapPoint>?>(null) }
    val routeEvaluations = remember { mutableStateOf<List<RouteEvaluation>?>(null) }
    val selectedRoute = remember { mutableStateOf<RouteEvaluation?>(null) }
    val fromPointState = remember { mutableStateOf<MapPoint?>(null) }
    val toPointState = remember { mutableStateOf<MapPoint?>(null) }
    val routingError = remember { mutableStateOf<String?>(null) }

    // Simple geocoding/hypothesis: check if coordinates or known addresses
    fun resolveAddress(address: String, lat: Double = 55.7522, lon: Double = 37.6133): MapPoint {
        val trimmed = address.trim()
        // If it looks like a coordinate (has comma + dot), parse it
        return if (trimmed.contains(",") && trimmed.contains(".")) {
            val parts = trimmed.split(",")
            if (parts.size >= 2) {
                val lat = parts[0].trim().toDoubleOrNull() ?: lat
                val lon = parts[1].trim().toDoubleOrNull() ?: lon
                MapPoint(lat, lon)
            } else {
                MapPoint(lat, lon)
            }
        } else {
            // Default to Moscow center for now (no external geocoding API available)
            MapPoint(lat, lon)
        }
    }

    // Generate a simple polyline between two points (mock routing)
    fun generateMockRoute(from: MapPoint, to: MapPoint): List<MapPoint> {
        val points = mutableListOf<MapPoint>()
        // Sample every ~0.01 degrees along the route
        val step = 0.005
        var lat = from.latitude
        var lon = from.longitude

        while (lat < to.latitude || lon < to.longitude) {
            if (lat < to.latitude) lat = (lat + step).coerceAtMost(to.latitude)
            if (lon < to.longitude) lon = (lon + step).coerceAtMost(to.longitude)
            points.add(MapPoint(lat, lon))
            if (points.size > 500) break
        }

        // If we didn't reach the target, add the direct points
        if (points.isEmpty() || points.last() != to) {
            points.add(to)
        }

        // Deduplicate and ensure we start from the origin
        val unique = mutableListOf<MapPoint>()
        points.forEach { pt ->
            if (unique.isEmpty() || unique.last() != pt) {
                unique.add(pt)
            }
        }
        return unique
    }

    fun buildRoute() {
        if (isLoading.value) return

        isLoading.value = true
        routePoints.value = null
        routeEvaluations.value = null
        selectedRoute.value = null
        routingError.value = null

        // Resolve start and end points
        val from = if (fromText.value == "Текущее положение") {
            resolveAddress("55.7522, 37.6133")
        } else {
            resolveAddress(fromText.value)
        }

        val to = resolveAddress(toText.value)
        fromPointState.value = from
        toPointState.value = to

        coroutineScope.launch {
            try {
                // Try real OSRM routing if available
                if (osrmRoutingSource != null && routeEvaluationEngine != null && routeComparisonEngine != null) {
                    val osrmRoutes = osrmRoutingSource.route(from, to, profile = RouteProfile.DRIVING)

                    if (osrmRoutes != null && osrmRoutes.isNotEmpty()) {
                        val evaluations = mutableListOf<RouteEvaluation>()
                        for ((index, osrmRoute) in osrmRoutes.withIndex()) {
                            val category = when (index) {
                                0 -> RouteCategory.FASTEST
                                1 -> RouteCategory.LOWEST_EXPOSURE
                                else -> RouteCategory.BALANCED
                            }
                            val eval = routeEvaluationEngine.evaluateRoute(
                                id = "route_$index",
                                geometry = osrmRoute.geometry,
                                durationSeconds = osrmRoute.durationSeconds,
                                category = category,
                                profile = RouteProfile.DRIVING
                            )
                            evaluations.add(eval)
                        }

                        // Re-classify properly using comparison engine
                        val classified = routeComparisonEngine.classifyRoutes(evaluations)
                        routeEvaluations.value = classified

                        // Select best route
                        val best = routeComparisonEngine.selectBestRoute(classified)
                        selectedRoute.value = best

                        // Use the best route's geometry for map display
                        routePoints.value = best?.geometry ?: classified.firstOrNull()?.geometry
                    } else {
                        // OSRM unavailable - fallback to mock
                        routingError.value = "Сервер маршрутизации недоступен. Используется упрощённый маршрут."
                        val points = generateMockRoute(from, to)
                        routePoints.value = points
                    }
                } else {
                    // No routing dependencies - fallback to mock
                    val points = generateMockRoute(from, to)
                    routePoints.value = points
                }
            } catch (e: Exception) {
                routingError.value = "Ошибка построения маршрута: ${e.message}"
                val points = generateMockRoute(from, to)
                routePoints.value = points
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
        ) {
            Text(
                text = "Eco Route Planner",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Постройте маршрут с минимальным воздействием на здоровье",
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
                            text = "Откуда",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(RgDimens.Space2))
                        OutlinedTextField(
                            value = fromText.value,
                            onValueChange = { fromText.value = it },
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .focusRequester(FocusRequester()),
                            label = { Text("Например: Текущее положение, Ковров, Москва") },
                            placeholder = {
                                Text(
                                    text = "Текущее положение",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { fromText.value = fromText.value }),
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
                            text = "Куда",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(RgDimens.Space2))
                        OutlinedTextField(
                            value = toText.value,
                            onValueChange = { toText.value = it },
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .focusRequester(FocusRequester()),
                            label = { Text("Адрес назначения") },
                            placeholder = {
                                Text(
                                    text = "Адрес назначения",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { toText.value = toText.value }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RgColor.Primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(RgDimens.Space4))

                    // Build route button
                    Button(
                        onClick = { buildRoute() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary)
                    ) {
                        Text(
                            text = if (isLoading.value) "Построение..." else "Построить маршрут",
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
                }
            }

            Spacer(modifier = Modifier.height(RgDimens.Space4))

            // Route comparison cards (multiple routes)
            routeEvaluations.value?.let { evaluations ->
                if (evaluations.size > 1) {
                    Text(
                        text = "Доступные маршруты",
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
                                                RouteCategory.FASTEST -> "Быстрый маршрут"
                                                RouteCategory.LOWEST_EXPOSURE -> "Экологичный маршрут"
                                                RouteCategory.BALANCED -> "Сбалансированный"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = eval.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${(eval.exposureScore).roundToInt()}",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = when {
                                                eval.exposureScore < 30 -> RgColor.ExposureGood
                                                eval.exposureScore < 60 -> RgColor.ExposureModerate
                                                else -> RgColor.ExposureHigh
                                            }
                                        )
                                        Text(
                                            text = "экспозиция",
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
                            text = "Маршрут построен (${(routePoints.value?.size ?: 0)} точек)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space3))

                        // Route summary (visual route rendered on map)
                        MapView(
                            modifier = Modifier.fillMaxSize().height(300.dp),
                            onMapReady = { mapLibreMap ->
                                val ctx = context
                                // Add location markers for from/to points
                                fromPointState.value?.let { fromPt ->
                                    mapLibreMap.addLocationMarker(ctx, fromPt.latitude, fromPt.longitude, com.roadguide.app.R.drawable.ic_launcher_background)
                                }
                                toPointState.value?.let { toPt ->
                                    mapLibreMap.addLocationMarker(ctx, toPt.latitude, toPt.longitude, com.roadguide.app.R.drawable.ic_launcher_background)
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
                                    text = "Длина маршрута",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val distKm = if (selected != null) (selected.distanceMeters / 1000.0) else 5.4
                                Text(
                                    text = "~${"%.1f".format(distKm)} км",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Оценка экологичности",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val exposure = selected?.exposureScore ?: 50.0
                                val exposureLabel = when {
                                    exposure < 30 -> "Отличная"
                                    exposure < 60 -> "Умеренная"
                                    else -> "Повышенная"
                                }
                                Text(
                                    text = exposureLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = when {
exposure < 30 -> RgColor.ExposureGood
                                    exposure < 60 -> RgColor.ExposureModerate
                                    else -> RgColor.ExposureHigh
                                    }
                                )
                            }
                        }

                        // Eco route warning from comparison engine
                        routeEvaluations.value?.let { evaluations ->
                            routeComparisonEngine?.let { comparison ->
                                comparison.getEcoRouteWarning(evaluations)?.let { warning ->
Spacer(modifier = Modifier.height(RgDimens.Space3))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Text(
                                            text = warning,
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
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Введите адреса откуда/куда и нажмите «Построить маршрут»",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
