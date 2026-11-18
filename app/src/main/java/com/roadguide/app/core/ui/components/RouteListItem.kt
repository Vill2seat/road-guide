package com.roadguide.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.domain.model.RouteSummary

@Composable
fun RouteListItem(
    route: RouteSummary,
    aqiLabel: String,
    modifier: Modifier = Modifier
) {
    val exposureColor = exposureColorFor(route.averageAqi?.toInt(), MaterialTheme.colorScheme.onSurfaceVariant)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route.routeType,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = route.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = RgColor.Success
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column() {
                    Text(
                        text = "${route.distanceMeters.toInt()} м",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Дистанция",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(RgDimens.Space4))

                Column() {
                    Text(
                        text = "${route.durationSeconds / 60} мин",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Длительность",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(RgDimens.Space4))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = route.averageAqi?.let { "${it.toInt()} AQI" } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = exposureColor
                    )
                    Text(
                        text = aqiLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun exposureColorFor(aqi: Int?, onSurfaceVariant: Color): Color {
    return if (aqi == null) {
        onSurfaceVariant
    } else when (aqi) {
        in 0..50 -> RgColor.ExposureGood
        in 51..100 -> RgColor.ExposureModerate
        in 101..150 -> RgColor.ExposureHigh
        else -> RgColor.ExposureVeryHigh
    }
}
