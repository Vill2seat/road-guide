package com.roadguide.app.feature.dailyreport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.data.local.entity.DailyReport
import com.roadguide.app.feature.dailyreport.viewmodel.DailyReportViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun DailyReportScreen(
    modifier: Modifier = Modifier
) {
    val dailyReportViewModel: DailyReportViewModel = hiltViewModel()

    val uiState by dailyReportViewModel.uiState.collectAsState()
    val report by dailyReportViewModel.report.collectAsState()
    val recentReports by dailyReportViewModel.recentReports.collectAsState()
    val selectedDate by dailyReportViewModel.selectedDate.collectAsState()
    val displayDate = remember(selectedDate) {
        runCatching {
            LocalDate.parse(selectedDate)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
        }.getOrDefault(selectedDate)
    }

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
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(RgDimens.Space4)
                    ) {
                        Text(
                            text = stringResource(R.string.daily_report_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(RgDimens.Space2))
                        Text(
                            text = stringResource(R.string.daily_report_day_label, displayDate),
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
                        }

                        RecentReportsList(
                            reports = recentReports,
                            selectedDate = selectedDate,
                            onSelectDate = dailyReportViewModel::loadReportForDate
                        )
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RgIcon.RgIconComposable(
            icon = RgIcon.Calendar,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(RgDimens.Space3))
        Text(
            text = stringResource(R.string.daily_report_no_exposure_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.daily_report_no_exposure_desc),
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
                text = stringResource(R.string.daily_report_score_title),
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
                    label = stringResource(R.string.daily_report_confidence),
                    value = formatDailyPercentage(report.confidence),
                    color = qualityColorFor(report.confidence)
                )
                StatChip(
                    label = stringResource(R.string.daily_report_coverage),
                    value = formatDailyPercentage(report.coverage),
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
                text = stringResource(R.string.daily_report_stats_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RgDimens.Space2)
            ) {
                StatItem(
                    label = stringResource(R.string.daily_report_distance),
                    value = stringResource(
                        R.string.daily_report_distance_km,
                        report.totalDistanceMeters / 1000.0
                    ),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = stringResource(R.string.daily_report_travel_time),
                    value = formatDuration(report.totalDurationSeconds),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RgDimens.Space2)
            ) {
                StatItem(
                    label = stringResource(R.string.daily_report_places),
                    value = report.visitedPlacesCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = stringResource(R.string.daily_report_coverage),
                    value = formatDailyPercentage(report.coverage),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RgDimens.Space2)
            ) {
                StatItem(
                    label = stringResource(R.string.daily_report_walking),
                    value = formatDuration(report.walkingDurationSeconds),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = stringResource(R.string.daily_report_transport),
                    value = formatDuration(report.transportDurationSeconds),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = stringResource(R.string.daily_report_stationary),
                    value = formatDuration(report.stationaryDurationSeconds),
                    modifier = Modifier.weight(1f)
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
                text = stringResource(R.string.daily_report_recommendations_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(RgDimens.Space3))

            if (recommendations.isEmpty()) {
                Text(
                    text = stringResource(R.string.daily_report_recommendations_empty),
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
private fun RecentReportsList(
    reports: List<DailyReport>,
    selectedDate: String,
    onSelectDate: (String) -> Unit
) {
    if (reports.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.daily_report_recent_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = RgDimens.Space2)
        )

        reports.forEach { report ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RgDimens.Space1),
                onClick = { onSelectDate(report.date) },
                colors = CardDefaults.cardColors(
                    containerColor = if (report.date == selectedDate) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RgDimens.Space2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = report.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.daily_report_score_value,
                                report.totalScore.toInt()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = scoreColorFor(report.totalScore)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = stringResource(
                            R.string.daily_report_distance_km,
                            report.totalDistanceMeters / 1000.0
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }
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
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
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

@Composable
private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    return stringResource(R.string.daily_report_minutes, minutes)
}

private fun scoreColorFor(score: Double): Color {
    return when (exposureScoreBand(score)) {
        ExposureScoreBand.LOW -> RgColor.ExposureGood
        ExposureScoreBand.MODERATE -> RgColor.ExposureModerate
        ExposureScoreBand.HIGH -> RgColor.ExposureHigh
        ExposureScoreBand.VERY_HIGH -> RgColor.ExposureVeryHigh
    }
}

private fun qualityColorFor(value: Double): Color {
    return when (dataQualityBand(value)) {
        DataQualityBand.HIGH -> RgColor.ExposureGood
        DataQualityBand.MODERATE -> RgColor.ExposureModerate
        DataQualityBand.LOW -> RgColor.ExposureHigh
        DataQualityBand.VERY_LOW -> RgColor.ExposureVeryHigh
    }
}

private fun parseRecommendations(json: String): List<String> {
    val trimmed = json.trim()
    if (trimmed == "[]" || trimmed.isEmpty()) return emptyList()
    val content = trimmed.removeSurrounding("[", "]")
    return content.split(",").map { it.trim().removeSurrounding("\"", "\"") }.filter { it.isNotEmpty() }
}
