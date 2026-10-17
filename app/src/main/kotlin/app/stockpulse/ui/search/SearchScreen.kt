package app.stockpulse.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.stockpulse.ui.UiState
import app.stockpulse.ui.home.ErrorState

@Composable
fun SearchScreen(viewModel: SearchViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val query by viewModel.query.collectAsState()
    val hits by viewModel.hits.collectAsState()
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = {},
                expanded = true,
                onExpandedChange = { if (!it) onBack() },
                placeholder = { Text("Search any stock or ETF") },
                leadingIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        expanded = true,
        onExpandedChange = { if (!it) onBack() },
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val s = hits) {
            UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
                LoadingIndicator(Modifier.padding(top = 32.dp))
            }
            is UiState.Error -> ErrorState(s.message, viewModel::retry)
            is UiState.Success -> if (s.value.isEmpty() && query.isNotBlank()) {
                Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
                    Text("No matches for \"$query\"", Modifier.padding(32.dp))
                }
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
