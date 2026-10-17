package app.stockpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stockpulse.data.ChartRange
import app.stockpulse.data.StockRepository
import app.stockpulse.data.WatchlistStore
import app.stockpulse.domain.Quote
import app.stockpulse.ui.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WatchItem(val quote: Quote, val closes: List<Double>)

class HomeViewModel(
    private val repo: StockRepository,
    private val store: WatchlistStore,
    private val symbols: Flow<List<String>> = store.symbols,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<WatchItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<WatchItem>>> = _state

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    init {
        viewModelScope.launch { symbols.collectLatest { load(it) } }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                load(symbols.first())
            } finally {
                _refreshing.value = false
            }
        }
    }

    val favorites: StateFlow<Set<String>> = store.symbols.map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleFavorite(symbol: String) {
        viewModelScope.launch { if (symbol in favorites.value) store.remove(symbol) else store.add(symbol) }
    }

    private suspend fun load(symbols: List<String>) {
        if (symbols.isEmpty()) {
            _state.value = UiState.Success(emptyList())
            return
        }
        val results = coroutineScope { symbols.map { s -> async { repo.history(s, ChartRange.M1) } }.awaitAll() }
        val quotes = results.mapNotNull { r -> r.getOrNull()?.let { WatchItem(it.quote, it.bars.map { b -> b.close }) } }
        _state.value = if (quotes.isEmpty()) {
            UiState.Error(results.firstNotNullOfOrNull { it.exceptionOrNull()?.message } ?: "Could not load prices")
        } else {
            UiState.Success(quotes)
        }
    }
}
