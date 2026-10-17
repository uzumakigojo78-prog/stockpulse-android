package app.stockpulse.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stockpulse.domain.Quote
import app.stockpulse.ui.UiState
import app.stockpulse.ui.components.Sparkline
import app.stockpulse.ui.theme.changeColor
import java.util.Locale

fun formatPrice(v: Double): String = String.format(Locale.US, "%,.2f", v)

fun formatChange(q: Quote): String =
    String.format(Locale.US, "%+.2f (%+.2f%%)", q.change, q.changePct)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSearch: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("StockPulse") },
                subtitle = { Text("Prices & statistical forecasts") },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            when (val s = state) {
                UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { LoadingIndicator() }
                is UiState.Error -> ErrorState(s.message, viewModel::refresh)
                is UiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { SearchEntry(onSearch) }
                    items(s.value, key = { it.quote.symbol }) { item ->
                        StockCard(item, onClick = { onOpen(item.quote.symbol) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, contentDescription = null)
            Text(
                "Search any stock or ETF",
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StockCard(item: WatchItem, onClick: () -> Unit) {
    val q = item.quote
    val color = changeColor(q.change)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    q.symbol.take(2),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(q.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    q.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Sparkline(item.closes, color, Modifier.width(72.dp).height(36.dp))
            Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End) {
                Text(formatPrice(q.price), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(formatChange(q), style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
    }
}
