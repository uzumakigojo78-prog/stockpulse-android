package app.stockpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stockpulse.data.StockRepository
import app.stockpulse.data.WatchlistStore
import app.stockpulse.domain.Quote
import app.stockpulse.ui.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: StockRepository,
    private val store: WatchlistStore,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Quote>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Quote>>> = _state

    init {
        viewModelScope.launch { store.symbols.collectLatest { load(it) } }
    }

    fun refresh() {
        viewModelScope.launch { load(store.symbols.first()) }
    }

    fun remove(symbol: String) {
        viewModelScope.launch { store.remove(symbol) }
    }

    private suspend fun load(symbols: List<String>) {
        if (symbols.isEmpty()) {
            _state.value = UiState.Success(emptyList())
            return
        }
        val results = coroutineScope { symbols.map { s -> async { repo.quote(s) } }.awaitAll() }
        val quotes = results.mapNotNull { it.getOrNull()?.quote }
        _state.value = if (quotes.isEmpty()) {
            UiState.Error(results.firstNotNullOfOrNull { it.exceptionOrNull()?.message } ?: "Could not load prices")
        } else {
            UiState.Success(quotes)
        }
    }
}
