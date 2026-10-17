package app.stockpulse.ui

import app.stockpulse.data.StockRepository
import app.stockpulse.ui.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun searchDebouncesRapidTyping() = runTest {
        val api = FakeStockApi()
        val vm = SearchViewModel(StockRepository(api))
        vm.onQueryChange("a"); advanceTimeBy(100)
        vm.onQueryChange("ap"); advanceTimeBy(100)
        vm.onQueryChange("app")
        advanceUntilIdle()
        assertEquals(1, api.searchCalls)
        assertEquals(listOf("AAPL"), (vm.hits.value as UiState.Success).value.map { it.symbol })
    }

    @Test
    fun blankQueryMakesNoCallAndClearsResults() = runTest {
        val api = FakeStockApi()
        val vm = SearchViewModel(StockRepository(api))
        vm.onQueryChange("   ")
        advanceUntilIdle()
        assertEquals(0, api.searchCalls)
        assertEquals(emptyList<Any>(), (vm.hits.value as UiState.Success).value)
    }
}
