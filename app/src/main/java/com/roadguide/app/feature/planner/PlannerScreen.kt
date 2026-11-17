package com.roadguide.app.feature.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon

@Composable
fun PlannerScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Text(
                text = "Eco Route Planner",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Постройте маршрут с минимальным воздействием на здоровье",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = RgDimens.Space4)
            )

            RouteInputCard()

            Spacer(modifier = Modifier.height(RgDimens.Space4))

            RouteAlternativesCard()
        }
    }
}

@Composable
private fun RouteInputCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = RgDimens.Space3),
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
                    text = "Откуда",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(RgDimens.Space2))
                Text(
                    text = "Текущее положение",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Куда",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(RgDimens.Space2))
                Text(
                    text = "Адрес назначения",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary)
                ) {
                    Text("Построить маршрут", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun RouteAlternativesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Альтернативы маршрута",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(RgDimens.Space3))

            RouteAlternativeItem(
                label = "Самый зелёный",
                distance = "2.4 км",
                duration = "18 мин",
                exposureLevel = 0.2,
                exposureColor = RgColor.ExposureGood,
                isSelected = true,
                icon = RgIcon.Leaf
            )

            Spacer(modifier = Modifier.height(RgDimens.Space2))

            RouteAlternativeItem(
                label = "Быстрый",
                distance = "1.8 км",
                duration = "12 мин",
                exposureLevel = 0.6,
                exposureColor = RgColor.ExposureModerate,
                isSelected = false,
                icon = RgIcon.Car
            )

            Spacer(modifier = Modifier.height(RgDimens.Space2))

            RouteAlternativeItem(
                label = "Через парк",
                distance = "2.9 км",
                duration = "22 мин",
                exposureLevel = 0.15,
                exposureColor = RgColor.ExposureGood,
                isSelected = false,
                icon = RgIcon.Tree
            )
        }
    }
}

@Composable
private fun RouteAlternativeItem(
    label: String,
    distance: String,
    duration: String,
    exposureLevel: Double,
    exposureColor: Color,
    isSelected: Boolean,
    icon: RgIcon.IconData
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RgIcon.RgIconComposable(
            icon = icon,
            modifier = Modifier.size(RgDimens.IconMedium)
        )
        Spacer(modifier = Modifier.width(RgDimens.Space2))

        Column() {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$distance · $duration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${(exposureLevel * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = exposureColor
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(6.dp)
                    .padding(top = RgDimens.Space1)
                    .background(
                        color = exposureColor.copy(alpha = 0.2f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .width(((exposureLevel * 60).coerceAtMost(60.0)).toFloat().dp)
                        .height(6.dp)
                        .background(
                            color = exposureColor,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}
