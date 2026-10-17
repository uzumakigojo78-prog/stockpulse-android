package app.stockpulse.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stockpulse.data.StockRepository
import app.stockpulse.domain.SearchHit
import app.stockpulse.ui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repo: StockRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val retryTick = MutableStateFlow(0)

    val hits: StateFlow<UiState<List<SearchHit>>> = combine(
        _query.debounce(DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged(),
        retryTick,
    ) { q, _ -> q }
        .flatMapLatest { q ->
            flow {
                if (q.isEmpty()) {
                    emit(UiState.Success(emptyList()))
                } else {
                    emit(UiState.Loading)
                    emit(
                        repo.search(q).fold(
                            onSuccess = { UiState.Success(it) },
                            onFailure = { UiState.Error(it.message ?: "Search failed") },
                        ),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Success(emptyList()))

    fun retry() {
        retryTick.value++
    }

    fun onQueryChange(text: String) {
        _query.value = text
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
