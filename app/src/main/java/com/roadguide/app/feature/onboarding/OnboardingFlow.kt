package com.roadguide.app.feature.onboarding

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadguide.app.R
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.feature.onboarding.viewmodel.OnboardingViewModel
import com.roadguide.app.feature.permissions.PermissionScreen
import com.roadguide.app.domain.model.TrackingPreferences
import com.roadguide.app.domain.model.TrackingSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val permissionSource = onboardingViewModel.permissionSource
    val currentPageState = onboardingViewModel.currentPage.collectAsState()
    val currentPage = currentPageState.value
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val permissionLauncher: ActivityResultLauncher<Array<String>> = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allGranted = granted.values.all { it }
        if (allGranted) {
            coroutineScope.launch(Dispatchers.IO) {
                val deferred = onboardingViewModel.completeOnboarding()
                deferred.await()
                onFinished()
            }
        } else {
            showPermissionDeniedDialog = true
        }
    }

    if (showPermissionDeniedDialog) {
        PermissionDeniedDialog(
            onRetry = {
                showPermissionDeniedDialog = false
                val missing = permissionSource.getMissingPermissions()
                if (missing.isNotEmpty()) {
                    permissionLauncher.launch(missing.toTypedArray())
                }
            },
            onSkip = {
                showPermissionDeniedDialog = false
                coroutineScope.launch(Dispatchers.IO) {
                    val deferred = onboardingViewModel.completeOnboarding()
                    deferred.await()
                    onFinished()
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentPage) {
                0 -> WelcomeScreen(
                    onNext = { onboardingViewModel.nextPage() },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> InfoScreen(
                    onBack = { onboardingViewModel.previousPage() },
                    onNext = { onboardingViewModel.nextPage() },
                    modifier = Modifier.fillMaxSize()
                )
                2 -> PermissionScreen(
                    onGrantPermissions = {
                        val act = context as? Activity
                        if (act == null) return@PermissionScreen
                        val missing = permissionSource.getMissingPermissions()
                        if (missing.isNotEmpty()) {
                            permissionLauncher.launch(missing.toTypedArray())
                        } else {
                            coroutineScope.launch(Dispatchers.IO) {
                                val deferred = onboardingViewModel.completeOnboarding()
                                deferred.await()
                                onFinished()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                else -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PermissionDeniedDialog(
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_denied_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.permission_denied_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.permission_retry))
                }
                Button(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.permission_skip))
                }
            }
        }
    }
}