package app.stockpulse.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.stockpulse.data.ChartRange
import app.stockpulse.ui.UiState
import app.stockpulse.ui.home.ErrorState
import app.stockpulse.ui.home.formatChange
import app.stockpulse.ui.home.formatPrice
import app.stockpulse.ui.theme.changeColor

@Composable
fun DetailScreen(viewModel: DetailViewModel, symbol: String, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val starred by viewModel.inWatchlist.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(symbol) },
                subtitle = {
                    (state as? UiState.Success)?.let { Text(it.value.quote.name) }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleWatchlist) {
                        Icon(
                            if (starred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = if (starred) "Remove from watchlist" else "Add to watchlist",
                        )
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { LoadingIndicator() }
                is UiState.Error -> ErrorState(s.message, viewModel::retry)
                is UiState.Success -> DetailContent(s.value, viewModel::selectRange)
            }
        }
    }
}

@Composable
private fun DetailContent(data: DetailData, onRange: (ChartRange) -> Unit) {
    val q = data.quote
    val color = changeColor(data.chart.bars.last().close - data.chart.bars.first().close)
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(formatPrice(q.price), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(formatChange(q), style = MaterialTheme.typography.titleSmall, color = changeColor(q.change))
        }
        PriceChart(data.chart.bars.map { it.close }, color, Modifier.fillMaxWidth().height(200.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ChartRange.entries.forEach { r ->
                ToggleButton(
                    checked = r == data.range,
                    onCheckedChange = { onRange(r) },
                    shapes = ToggleButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f),
                ) { Text(r.label) }
            }
        }
        ForecastCard(data.forecast)
    }
}
