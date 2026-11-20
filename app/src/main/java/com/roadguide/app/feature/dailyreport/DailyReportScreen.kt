package com.roadguide.app.feature.dailyreport

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.data.local.entity.DailyReport
import com.roadguide.app.feature.dailyreport.viewmodel.DailyReportViewModel

@Composable
fun DailyReportScreen(
    modifier: Modifier = Modifier
) {
    val dailyReportViewModel: DailyReportViewModel = hiltViewModel()

    val uiState by dailyReportViewModel.uiState.collectAsState()
    val report by dailyReportViewModel.report.collectAsState()
    val recentReports by dailyReportViewModel.recentReports.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = state.message,
                    onRetry = { dailyReportViewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    val reportValue = report
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = stringResource(R.string.daily_report_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space2))
                        Text(
                            text = reportValue?.date ?: "Сегодня",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space4))

                        if (reportValue == null) {
                            EmptyDailyReportState()
                        } else {
                            ScoreCard(report = reportValue)
                            Spacer(modifier = Modifier.height(RgDimens.Space4))
                            StatisticsCard(report = reportValue)
                            Spacer(modifier = Modifier.height(RgDimens.Space4))
                            RecommendationsCard(report = reportValue)
                            Spacer(modifier = Modifier.height(RgDimens.Space4))
                            RecentReportsList(reports = recentReports)
                        }

                        Button(
                            onClick = { dailyReportViewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.daily_report_refresh), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDailyReportState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RgIcon.RgIconComposable(
            icon = RgIcon.Calendar,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(RgDimens.Space3))
        Text(
            text = "Нет отчёта за этот день",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Завершите день трекинга, чтобы увидеть детальный отчёт",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScoreCard(report: DailyReport) {
    val scoreColor = scoreColorFor(report.totalScore)

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
                text = "Оценка дня",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(RgDimens.Space2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${report.totalScore.toInt()}",
                    style = MaterialTheme.typography.displaySmall,
                    color = scoreColor
                )
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                StatChip(
                    label = "Надёжность",
                    value = "${(report.confidence * 100).toInt()}%",
                    color = qualityColorFor(report.confidence)
                )
                StatChip(
                    label = "Покрытие",
                    value = "${(report.coverage * 100).toInt()}%",
                    color = qualityColorFor(report.coverage)
                )
            }
        }
    }
}

@Composable
private fun StatisticsCard(report: DailyReport) {
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
                text = "Статистика дня",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Дистанция",
                    value = "${(report.totalDistanceMeters / 1000).toString().take(4)} км"
                )
                StatItem(
                    label = "Время в пути",
                    value = formatDuration(report.totalDurationSeconds)
                )
                StatItem(
                    label = "Места",
                    value = report.visitedPlacesCount.toString()
                )
                StatItem(
                    label = "Coverage",
                    value = "${report.averageCoverage?.toInt() ?: 0}%"
                )
                StatItem(
                    label = "Coverage",
                    value = "${report.averageCoverage?.toInt() ?: 0}%"
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Ходьба",
                    value = formatDuration(report.walkingDurationSeconds)
                )
                StatItem(
                    label = "Транспорт",
                    value = formatDuration(report.transportDurationSeconds)
                )
                StatItem(
                    label = "Стойка",
                    value = formatDuration(report.stationaryDurationSeconds)
                )
            }
        }
    }
}

@Composable
private fun RecommendationsCard(report: DailyReport) {
    val recommendations = parseRecommendations(report.recommendationsJson)

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
                text = "Рекомендации",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(RgDimens.Space3))

            if (recommendations.isEmpty()) {
                Text(
                    text = "Рекомендации будут доступны после накопления достаточного количества данных",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recommendations.forEach { rec ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RgIcon.RgIconComposable(
                                icon = RgIcon.Info,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(RgDimens.Space2))
                            Text(
                                text = rec,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(RgDimens.Space2))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentReportsList(reports: List<DailyReport>) {
    if (reports.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Недавние отчёты",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = RgDimens.Space2)
        )

        reports.forEach { report ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column() {
                    Text(
                        text = report.date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${report.totalScore.toInt()}/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = scoreColorFor(report.totalScore)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${(report.totalDistanceMeters / 1000).toString().take(4)} км",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space1))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    return "${minutes} мин"
}

private fun scoreColorFor(score: Double): Color {
    return when {
        score >= 75.0 -> RgColor.ExposureGood
        score >= 50.0 -> RgColor.ExposureModerate
        score >= 25.0 -> RgColor.ExposureHigh
        else -> RgColor.ExposureVeryHigh
    }
}

private fun qualityColorFor(value: Double): Color {
    return when {
        value >= 0.8 -> RgColor.ExposureGood
        value >= 0.5 -> RgColor.ExposureModerate
        value >= 0.2 -> RgColor.ExposureHigh
        else -> RgColor.ExposureVeryHigh
    }
}

private fun parseRecommendations(json: String): List<String> {
    val trimmed = json.trim()
    if (trimmed == "[]" || trimmed.isEmpty()) return emptyList()
    val content = trimmed.removeSurrounding("[", "]")
    return content.split(",").map { it.trim().removeSurrounding("\"", "\"") }.filter { it.isNotEmpty() }
}
