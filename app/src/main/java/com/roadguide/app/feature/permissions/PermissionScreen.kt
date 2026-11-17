package com.roadguide.app.feature.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon

/**
 * Permission onboarding screen.
 *
 * Shown on first launch or when required permissions are missing.
 * Explains why Road Guide needs location and notification permissions
 * and requests them via [onGrantPermissions].
 */
@Composable
fun PermissionScreen(
    onGrantPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            // Icon card
            Card(
                modifier = Modifier.size(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                RgIcon.RgIconComposable(
                    icon = RgIcon.Shield,
                    modifier = Modifier.size(56.dp)
                )
            }

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(RgDimens.Space4)
            )

            // Title
            Text(
                text = "Разрешения для Road Guide",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(RgDimens.Space2)
            )

            // Description
            Text(
                text = """
                    Road Guide нуждается в доступе к вашему местоположению и уведомлениям,
                    чтобы автоматически отслеживать ваше дневное воздействие на окружающую среду
                    в фоновом режиме, даже когда приложение свернуто.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(RgDimens.Space4)
            )

            // Permission cards
            PermissionCard(
                icon = RgIcon.MapPin,
                title = "Местоположение",
                description = "Для автоматической записи маршрутов и расчёта воздействия в реальном времени"
            )

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(RgDimens.Space2)
            )

            PermissionCard(
                icon = RgIcon.Bell,
                title = "Уведомления",
                description = "Для оповещений о превышении порога воздействия и фоновой работы трекера"
            )

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(RgDimens.Space4)
            )

            // Grant button
            Button(
                onClick = onGrantPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Дать разрешения", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Single permission explanation card.
 */
@Composable
private fun PermissionCard(
    icon: com.roadguide.app.core.ui.theme.RgIcon.IconData,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            com.roadguide.app.core.ui.theme.RgIcon.RgIconComposable(
                icon = icon,
                modifier = Modifier.size(32.dp)
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(RgDimens.Space2))

            Column() {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
