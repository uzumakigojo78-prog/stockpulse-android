package app.stockpulse.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.stockpulse.ui.UiState
import app.stockpulse.ui.home.ErrorState

@Composable
fun SearchScreen(viewModel: SearchViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val query by viewModel.query.collectAsState()
    val hits by viewModel.hits.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Surface(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Column(Modifier.systemBarsPadding()) {
            TextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search any stock or ETF") },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                leadingIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(focus),
            )
            when (val s = hits) {
                UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
                    LoadingIndicator(Modifier.padding(top = 32.dp))
                }
                is UiState.Error -> ErrorState(s.message, viewModel::retry)
                is UiState.Success -> if (s.value.isEmpty() && query.isNotBlank()) {
                    Text("No matches for \"$query\"", Modifier.padding(32.dp))
                } else {
                    LazyColumn {
                        items(s.value, key = { it.symbol }) { hit ->
                            ListItem(
                                headlineContent = { Text(hit.symbol) },
                                supportingContent = { Text("${hit.name} · ${hit.exchange}") },
                                leadingContent = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                modifier = Modifier.clickable { onOpen(hit.symbol) },
                            )
                        }
                    }
                }
            }
        }
    }
}
