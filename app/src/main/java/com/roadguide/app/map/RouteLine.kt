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
import kotlin.math.min

/**
 * Simple map point for route rendering without MapLibre SDK dependency.
 * Replace with org.maplibre.android.geometry.LatLng when SDK repository is available.
 */
data class MapPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * Composable component for rendering a route polyline on the map.
 * Supports gradient coloring based on exposure/activity level (optional).
 */
@Composable
fun RouteLine(
    routePoints: List<MapPoint>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6FAF8E),
    strokeWidth: Float = 6f,
    showProgress: Boolean = false,
    totalPoints: Int = 0
) {
    val path = remember { Path() }
    val animatedProgress = remember { Animatable(0f) }

    // Create a path from the route points
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

    // Animate progress if needed
    LaunchedEffect(showProgress, routePoints.size) {
        if (showProgress && routePoints.isNotEmpty()) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw the route line
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth)
        )

        // Draw progress indicator (if animated)
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

    // Notify on route change
    LaunchedEffect(routePoints, showProgress) {
        // Emit route update
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
