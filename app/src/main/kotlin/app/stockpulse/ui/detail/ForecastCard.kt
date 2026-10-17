package app.stockpulse.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.stockpulse.domain.ForecastResult
import app.stockpulse.ui.home.formatPrice
import java.util.Locale

@Composable
fun ForecastCard(forecast: ForecastResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Forecast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            when (forecast) {
                ForecastResult.InsufficientData ->
                    Text("Not enough history to estimate a forecast for this symbol.")
                is ForecastResult.Ok -> forecast.horizons.forEach { h ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (h.days == 1) "1 day" else "${h.days} days", fontWeight = FontWeight.SemiBold)
                            Text(formatPrice(h.median), fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "80% range ${formatPrice(h.low)} – ${formatPrice(h.high)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        LinearWavyProgressIndicator(progress = { h.probUp.toFloat() }, modifier = Modifier.fillMaxWidth())
                        Text(
                            String.format(Locale.US, "%.0f%% chance higher", h.probUp * 100),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Text(
                "Statistical estimate, not financial advice.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
