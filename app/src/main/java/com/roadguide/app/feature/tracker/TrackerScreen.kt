package com.roadguide.app.feature.tracker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.EmptyPlaceholder
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder

@Composable
fun TrackerScreen(
    state: ScreenState = ScreenState.Empty,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (state) {
        ScreenState.Empty -> EmptyPlaceholder("Tracker", "Map view will be displayed here.", modifier)
        ScreenState.Loading -> LoadingPlaceholder(modifier)
        is ScreenState.Error -> ErrorPlaceholder(state.message, onRetry, modifier)
    }
}
