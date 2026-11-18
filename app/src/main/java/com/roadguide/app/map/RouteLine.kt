package com.roadguide.app.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Composable component for rendering a route polyline on the map canvas.
 * Uses Compose Canvas for rendering - the line is drawn in screen coordinates
 * and will not pan/zoom with the map. For MapLibre-native polylines, use
 * MapView's onMapReady callback to add polylines directly to the MapLibreMap.
 */
@Composable
fun RouteLine(
    routePoints: List<MapPoint>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6FAF8E),
    strokeWidth: Float = 6f,
    showProgress: Boolean = false
) {
    val path = remember { Path() }
    val animatedProgress = remember { Animatable(0f) }

    if (routePoints.isNotEmpty()) {
        path.reset()
        routePoints.firstOrNull()?.let { firstPoint ->
            path.moveTo(
                x = firstPoint.longitude.toFloat(),
                y = firstPoint.latitude.toFloat()
            )
            routePoints.drop(1).forEach { point ->
                path.lineTo(
                    x = point.longitude.toFloat(),
                    y = point.latitude.toFloat()
                )
            }
        }
    }

    LaunchedEffect(showProgress, routePoints.size) {
        if (showProgress && routePoints.isNotEmpty()) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth)
        )

        if (showProgress && routePoints.isNotEmpty()) {
            val progress = animatedProgress.value
            if (progress < 1f) {
                val endIndex = (routePoints.size * progress).toInt()
                val partialPath = Path()
                routePoints.take(endIndex).firstOrNull()?.let { firstPoint ->
                    partialPath.moveTo(
                        x = firstPoint.longitude.toFloat(),
                        y = firstPoint.latitude.toFloat()
                    )
                    routePoints.take(endIndex).drop(1).forEach { point ->
                        partialPath.lineTo(
                            x = point.longitude.toFloat(),
                            y = point.latitude.toFloat()
                        )
                    }
                }
                drawPath(
                    path = partialPath,
                    color = color.copy(alpha = 0.6f),
                    style = Stroke(width = strokeWidth + 4f)
                )
            }
        }
    }
}

/**
 * Data class for route line configuration.
 */
data class RouteLineData(
    val points: List<MapPoint>,
    val color: Color = Color(0xFF6FAF8E),
    val strokeWidth: Float = 6f
)
