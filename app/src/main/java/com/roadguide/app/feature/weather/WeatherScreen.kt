package com.roadguide.app.feature.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.feature.weather.viewmodel.WeatherViewModel

@Composable
fun WeatherScreen(
    modifier: Modifier = Modifier
) {
    val weatherViewModel: WeatherViewModel = hiltViewModel()

    val uiState by weatherViewModel.uiState.collectAsState()
    val sample by weatherViewModel.sample.collectAsState()

    val uiStateValue = uiState

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (uiStateValue) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = uiStateValue.message,
                    onRetry = { weatherViewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Weather & Air Quality",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space2))
                        Text(
                            text = sample?.let { formatLocation(it) } ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space4))

                        sample?.let { currentSample ->
                            WeatherHeaderCard(sample = currentSample)
                            Spacer(modifier = Modifier.height(RgDimens.Space4))
                            PollutantsCard(sample = currentSample)
                            Spacer(modifier = Modifier.height(RgDimens.Space4))
                            WeatherDetailsCard(sample = currentSample)
                            Spacer(modifier = Modifier.height(RgDimens.Space4))

                            Button(
                                onClick = { weatherViewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.weather_refresh), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWeatherState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RgIcon.RgIconComposable(
            icon = RgIcon.Cloud,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(RgDimens.Space3))
        Text(
            text = "Нет данных о погоде",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Нажмите «Обновить данные» для загрузки текущей погоды и качества воздуха",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeatherHeaderCard(sample: EnvironmentalSample) {
    val aqi = sample.europeanAqi ?: sample.usAqi ?: 0
    val aqiColor = aqiColorFor(aqi)
    val aqiLabel = aqiLabelFor(aqi)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column() {
                    Text(
                        text = "Качество воздуха",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = aqi.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = aqiColor
                    )
                    Text(
                        text = aqiLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = sample.dataQuality.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = qualityColorFor(sample.dataQuality)
                    )
                    Text(
                        text = sample.source.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PollutantsCard(sample: EnvironmentalSample) {
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
                text = "Загрязняющие вещества",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PollutantItem(
                    label = "PM2.5",
                    value = sample.pm2_5?.let { "${it.toInt()} µg/m³" } ?: "—",
                    unit = "WHO: 25"
                )
                PollutantItem(
                    label = "PM10",
                    value = sample.pm10?.let { "${it.toInt()} µg/m³" } ?: "—",
                    unit = "WHO: 50"
                )
                PollutantItem(
                    label = "NO₂",
                    value = sample.nitrogenDioxide?.let { "${it.toInt()} µg/m³" } ?: "—",
                    unit = "WHO: 40"
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PollutantItem(
                    label = "O₃",
                    value = sample.ozone?.let { "${it.toInt()} µg/m³" } ?: "—",
                    unit = "WHO: 100"
                )
                PollutantItem(
                    label = "SO₂",
                    value = sample.sulphurDioxide?.let { "${it.toInt()} µg/m³" } ?: "—",
                    unit = "WHO: 40"
                )
                PollutantItem(
                    label = "CO",
                    value = sample.carbonMonoxide?.let { "${it.toInt()} µg/m³" } ?: "—",
                    unit = "WHO: 4"
                )
            }
        }
    }
}

@Composable
private fun PollutantItem(label: String, value: String, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeatherDetailsCard(sample: EnvironmentalSample) {
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
                text = "Погодные условия",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherDetailItem(
                    label = "Температура",
                    value = sample.temperatureCelsius?.let { "${it.toInt()}°C" } ?: "—"
                )
                WeatherDetailItem(
                    label = "Ощущается как",
                    value = sample.apparentTemperatureCelsius?.let { "${it.toInt()}°C" } ?: "—"
                )
                WeatherDetailItem(
                    label = "Влажность",
                    value = sample.relativeHumidityPercent?.let { "${it.toInt()}%" } ?: "—"
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherDetailItem(
                    label = "Ветер",
                    value = sample.windSpeedMs?.let { "${it.toInt()} м/с" } ?: "—"
                )
                WeatherDetailItem(
                    label = "Облачность",
                    value = sample.cloudCoverPercent?.let { "${it.toInt()}%" } ?: "—"
                )
                WeatherDetailItem(
                    label = "УФ-индекс",
                    value = sample.uvIndex?.let { "${it.toInt()}" } ?: "—"
                )
            }
        }
    }
}

@Composable
private fun WeatherDetailItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatLocation(sample: EnvironmentalSample): String {
    return "${sample.latitude.toString().take(6)}, ${sample.longitude.toString().take(6)}"
}

private fun aqiColorFor(aqi: Int): Color {
    return when (aqi) {
        in 0..50 -> RgColor.ExposureGood
        in 51..100 -> RgColor.ExposureModerate
        in 101..150 -> RgColor.ExposureHigh
        else -> RgColor.ExposureVeryHigh
    }
}

private fun aqiLabelFor(aqi: Int): String {
    return when (aqi) {
        in 0..50 -> "Хорошо"
        in 51..100 -> "Умеренно"
        in 101..150 -> "Неблагоприятно для чувствительных"
        else -> "Неблагоприятно"
    }
}

private fun qualityColorFor(quality: DataQuality): Color {
    return when (quality) {
        DataQuality.HIGH -> RgColor.ExposureGood
        DataQuality.MEDIUM -> RgColor.ExposureModerate
        DataQuality.LOW -> RgColor.ExposureHigh
        DataQuality.UNKNOWN, DataQuality.STALE, DataQuality.OUT_OF_RANGE -> RgColor.OnSurfaceVariant
    }
}
