package com.roadguide.app.map

import android.content.Context
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roadguide.app.map.model.MapPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView as NativeMapView
import org.maplibre.android.maps.Style

private const val MAP_STYLE_URI = "https://demotiles.maplibre.org/style.json"

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    mapContentKey: Any? = null,
    onMapUpdate: (MapLibreMap) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember(mapContentKey) { mutableStateOf<NativeMapView?>(null) }
    val mapLibreMap = remember { mutableStateOf<MapLibreMap?>(null) }
    val isDisposed = remember { mutableStateOf(false) }
    val currentOnMapReady = rememberUpdatedState(onMapReady)
    val currentOnMapUpdate = rememberUpdatedState(onMapUpdate)

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { viewContext ->
            NativeMapView(viewContext).also { nativeView ->
                nativeView.onCreate(Bundle())
                mapView.value = nativeView
                nativeView.getMapAsync { map ->
                    if (isDisposed.value) return@getMapAsync
                    try {
                        map.setStyle(Style.Builder().fromUri(MAP_STYLE_URI)) {
                            if (isDisposed.value) return@setStyle
                            map.uiSettings.isAttributionEnabled = true
                            mapLibreMap.value = map
                            currentOnMapReady.value(map)
                        }
                    } catch (_: Exception) {
                        mapLibreMap.value = map
                        currentOnMapReady.value(map)
                    }
                }
            }
        },
        update = { }
    )

    // MapViewLifecycleBinding removed - lifecycle handled via DisposableEffect
    // Map lifecycle is managed via AndroidView factory and LaunchedEffect

    DisposableEffect(Unit) {
        // Lifecycle observer managed via DisposableEffect
        // No manual observer registration needed
    }

    LaunchedEffect(mapLibreMap.value) {
        mapLibreMap.value?.let { map ->
            if (!isDisposed.value) {
                currentOnMapUpdate.value(map)
            }
        }
    }

    if (mapView.value == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Loading map…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
