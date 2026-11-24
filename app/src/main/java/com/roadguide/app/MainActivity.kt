package com.roadguide.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roadguide.app.core.ui.theme.RoadGuideTheme
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.data.source.OsrmRoutingSource
import com.roadguide.app.domain.usecase.impl.RouteComparisonEngine
import com.roadguide.app.domain.usecase.impl.RouteEvaluationEngine
import com.roadguide.app.feature.onboarding.OnboardingFlow
import com.roadguide.app.feature.permissions.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var permissionSource: PermissionSource
    @Inject lateinit var locationSource: LocationSource
    @Inject lateinit var osrmRoutingSource: OsrmRoutingSource
    @Inject lateinit var routeEvaluationEngine: RouteEvaluationEngine
    @Inject lateinit var routeComparisonEngine: RouteComparisonEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoadGuideTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionGate()
                }
            }
        }
    }

    @Composable
    private fun PermissionGate() {
        var hasPermissions by remember { mutableStateOf(permissionSource.hasAllPermissions()) }
        var pendingPermissionRequest by remember { mutableStateOf<Array<String>?>(null) }
        val lifecycleOwner = LocalLifecycleOwner.current

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            hasPermissions = permissionSource.hasAllPermissions()
            if (!hasPermissions) {
                val missing = permissionSource.getMissingPermissions()
                if (missing.isNotEmpty() && missing.none(granted::containsKey)) {
                    pendingPermissionRequest = missing.toTypedArray()
                }
            }
        }

        var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(permissionSource) {
            permissionSource.hasCompletedOnboarding().collect { onboardingDone = it }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasPermissions = permissionSource.hasAllPermissions()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(pendingPermissionRequest) {
            pendingPermissionRequest?.let { permissions ->
                pendingPermissionRequest = null
                permissionLauncher.launch(permissions)
            }
        }

        LaunchedEffect(hasPermissions) {
            if (!hasPermissions && permissionSource.hasAllPermissions()) {
                hasPermissions = true
            }
        }

        if (onboardingDone == null) {
            LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        } else if (onboardingDone == false) {
            OnboardingFlow(
                onFinished = {
                    hasPermissions = permissionSource.hasAllPermissions()
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (!hasPermissions) {
            PermissionScreen(
                onGrantPermissions = {
                    val missing = permissionSource.getMissingPermissions()
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    } else {
                        hasPermissions = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AppNavHost(
                permissionSource = permissionSource,
                permissionLauncher = permissionLauncher,
                locationSource = locationSource,
                osrmRoutingSource = osrmRoutingSource,
                routeEvaluationEngine = routeEvaluationEngine,
                routeComparisonEngine = routeComparisonEngine
            )
        }
    }
}