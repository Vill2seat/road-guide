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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.roadguide.app.core.ui.theme.RoadGuideTheme
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.feature.permissions.PermissionScreen

/**
 * Main entry point for Road Guide.
 *
 * Sets up the RoadGuideTheme (from Figma design tokens) and hosts the permission
 * gate or [AppNavHost] depending on whether required permissions are granted.
 */
class MainActivity : ComponentActivity() {

    private val permissionSource = PermissionSource(this)

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

    /**
     * Gates the app behind runtime permissions.
     * Shows PermissionScreen until all permissions are granted, then AppNavHost.
     */
    @Composable
    private fun PermissionGate() {
        val hasPermissions = remember { mutableStateOf(permissionSource.hasAllPermissions()) }
        val hasBeenAsked = remember { mutableStateOf(false) }

        // Register the permission result launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            val allGranted = granted.values.all { it }
            hasPermissions.value = allGranted
            hasBeenAsked.value = true
        }

        if (!hasPermissions.value) {
            PermissionScreen(
                onGrantPermissions = {
                    val missing = permissionSource.requestAllPermissions(this@MainActivity)
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    } else {
                        hasPermissions.value = true
                        hasBeenAsked.value = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AppNavHost()
        }
    }
}
