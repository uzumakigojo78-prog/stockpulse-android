package app.stockpulse.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stockpulse.data.ChartData
import app.stockpulse.data.ChartRange
import app.stockpulse.data.StockRepository
import app.stockpulse.data.WatchlistStore
import app.stockpulse.domain.ForecastEngine
import app.stockpulse.domain.ForecastResult
import app.stockpulse.domain.Quote
import app.stockpulse.ui.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [quote] carries the day change; [chart] is the series for the selected [range]. */
data class DetailData(
    val quote: Quote,
    val chart: ChartData,
    val range: ChartRange,
    val forecast: ForecastResult,
)

class DetailViewModel(
    private val repo: StockRepository,
    private val engine: ForecastEngine,
    private val store: WatchlistStore,
    private val symbol: String,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<DetailData>>(UiState.Loading)
    val state: StateFlow<UiState<DetailData>> = _state

    val inWatchlist: StateFlow<Boolean> = store.symbols
        .map { symbol.uppercase() in it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var range = ChartRange.M6
    private var job: Job? = null

    init {
        load()
    }

    fun selectRange(newRange: ChartRange) {
        range = newRange
        load()
    }

    fun retry() = load()

    fun toggleWatchlist() {
        viewModelScope.launch {
            if (inWatchlist.value) store.remove(symbol) else store.add(symbol)
        }
    }

    private fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = UiState.Loading
            val quote = repo.quote(symbol).getOrElse { return@launch fail(it) }.quote
            val daily = repo.history(symbol, ChartRange.Y1).getOrElse { return@launch fail(it) }
            val chart = if (range == ChartRange.Y1) daily else {
                repo.history(symbol, range).getOrElse { return@launch fail(it) }
            }
            _state.value = UiState.Success(DetailData(quote, chart, range, engine.forecast(daily.bars)))
        }
    }

    private fun fail(e: Throwable) {
        _state.value = UiState.Error(e.message ?: "Something went wrong")
    }
}
