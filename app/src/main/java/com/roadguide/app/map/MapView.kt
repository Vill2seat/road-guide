package com.roadguide.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Composable wrapper for map view.
 *
 * Note: MapLibre Android SDK requires a custom Maven repository.
 * To enable real MapLibre integration:
 * 1. Add to settings.gradle.kts dependencyResolutionManagement:
 *    maven { url = uri("https://maven.maplibre.org") }
 * 2. Add implementation(libs.maplibre.android) to app/build.gradle.kts dependencies
 * 3. Replace this placeholder with AndroidView wrapping MapView
 *
 * Currently renders a placeholder canvas ready for MapLibre integration.
 */
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    initialCamera: Any? = null,
    testTag: String = "mapView"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag)
            .background(androidx.compose.ui.graphics.Color(0xFFE8F5E9))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "Map View",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            androidx.compose.material3.Text(
                text = "Configure maven.maplibre.org repo for MapLibre SDK",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }
    }
}
