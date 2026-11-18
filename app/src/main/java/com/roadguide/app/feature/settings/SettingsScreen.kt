package com.roadguide.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings screen - separate from Profile.
 * Displays tracking, privacy, and environmental settings.
 */
@Composable
fun SettingsScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Настройки трекинга, приватности и окружения",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
