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
import androidx.compose.foundation.layout.width
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
fun InfoScreen(
    onBack: () -> Unit,
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
                icon = RgIcon.Info,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(RgDimens.Space4))

        // Title
        Text(
            text = "Как это работает",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(RgDimens.Space2))

        // Description
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoItem(
                icon = RgIcon.MapPin,
                title = "Автозапись маршрутов",
                description = "Приложение записывает ваши перемещения в фоне и рассчитывает воздействие на окружающую среду."
            )
            Spacer(modifier = Modifier.height(RgDimens.Space3))
            InfoItem(
                icon = RgIcon.Bell,
                title = "Уведомления",
                description = "Вы будете получать оповещения при превышении безопасного уровня воздействия."
            )
            Spacer(modifier = Modifier.height(RgDimens.Space3))
            InfoItem(
                icon = RgIcon.Shield,
                title = "Конфиденциальность",
                description = "Все данные хранятся локально на вашем устройстве. Ничего не отправляется в облако без вашего согласия."
            )
        }

        Spacer(modifier = Modifier.height(RgDimens.Space8))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.width(100.dp)
            ) {
                Text(stringResource(R.string.common_back), style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.common_next), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: com.roadguide.app.core.ui.theme.RgIcon.IconData,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.roadguide.app.core.ui.theme.RgIcon.RgIconComposable(
            icon = icon,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(RgDimens.Space2))
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
