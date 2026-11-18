package com.roadguide.app.feature.onboarding

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.roadguide.app.R
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon

@Composable
fun WelcomeScreen(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RgDimens.Space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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

        Spacer(modifier = Modifier.height(RgDimens.Space4))

        // Title
        Text(
            text = "Добро пожаловать в Road Guide",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(RgDimens.Space2))

        // Description
        Text(
            text = """
                Road Guide помогает отслеживать ваше воздействие на окружающую среду
                в течение дня. Приложение записывает ваши маршруты, оценивает качество
                воздуха и шума по местам пребывания и формирует дневные отчёты.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(RgDimens.Space8))

        // Next button
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.common_next), style = MaterialTheme.typography.labelLarge)
        }
    }
}
