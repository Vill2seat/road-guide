package com.roadguide.app.map

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback

/**
 * Holds a reference to the native MapLibre MapView instance so that
 * lifecycle callbacks can be forwarded from the Compose lifecycle.
 */
private class MapViewHolder(
    var view: org.maplibre.android.maps.MapView? = null
)

/**
 * Composable wrapper for MapLibre Android SDK map view.
 *
 * Uses a local AAR from `app/libs/maplibre-android-13.5.4.aar`
 * (maven.maplibre.org is DNS-unreachable in this environment).
 *
 * Style: https://demotiles.maplibre.org/style.json (free, no access token required).
 *
 * Lifecycle callbacks (onStart, onResume, onPause, onStop, onDestroy,
 * onLowMemory) are forwarded to the underlying MapLibre MapView via
 * [DefaultLifecycleObserver] to prevent native renderer crashes.
 */
@Composable
fun MapView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val lifecycle = lifecycleOwner?.lifecycle

    val holder = remember { MapViewHolder() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val mv = org.maplibre.android.maps.MapView(context).apply {
                onCreate(Bundle())
            }
            mv.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(mapLibreMap: MapLibreMap) {
                    mapLibreMap.setStyle("https://demotiles.maplibre.org/style.json")
                }
            })
            holder.view = mv
            mv
        }
    )

    DisposableEffect(lifecycle, holder) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                holder.view?.let { it.onStart() }
            }

            override fun onResume(owner: LifecycleOwner) {
                holder.view?.let { it.onResume() }
            }

            override fun onPause(owner: LifecycleOwner) {
                holder.view?.let { it.onPause() }
            }

            override fun onStop(owner: LifecycleOwner) {
                holder.view?.let { it.onStop() }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                holder.view?.let { it.onDestroy() }
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
            holder.view?.let { it.onLowMemory() }
            holder.view?.let { it.onDestroy() }
        }
    }
}
