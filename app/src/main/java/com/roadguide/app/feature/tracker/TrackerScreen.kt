package com.roadguide.app.feature.tracker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.EmptyPlaceholder
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.domain.model.RouteTab

/**
 * Tracker (Map) screen placeholder.
 *
 * Route: `tracker`
 * Tab: [RouteTab.TRACKER]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    state: ScreenState = ScreenState.Empty,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(text = "Tracker", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        TrackerScreenContent(
            state = state,
            onRetry = onRetry,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun TrackerScreenContent(
    state: ScreenState,
    onRetry: (() -> Unit)?,
    modifier: Modifier
) {
    when (state) {
        ScreenState.Empty -> {
            EmptyPlaceholder(
                title = "Tracker",
                message = "Map view will be displayed here.",
                modifier = modifier
            )
        }
        ScreenState.Loading -> {
            LoadingPlaceholder(modifier = modifier)
        }
        is ScreenState.Error -> {
            ErrorPlaceholder(
                message = state.message,
                onRetry = onRetry,
                modifier = modifier
            )
        }
    }
}