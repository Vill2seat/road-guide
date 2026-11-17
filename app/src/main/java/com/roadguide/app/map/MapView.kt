package com.roadguide.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

/**
 * Composable wrapper for map view.
 *
 * Empty container ready for MapLibre Android SDK integration via AndroidView.
 * Enable the MapLibre dependency in app/build.gradle.kts and settings.gradle.kts
 * when maven.maplibre.org is reachable, then replace this Box with the real MapView.
 */
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    testTag: String = "mapView"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag)
            .background(Color(0xFFE8F5E9))
    )
}
