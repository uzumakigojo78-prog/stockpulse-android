package app.stockpulse.ui

import app.stockpulse.data.StockException
import app.stockpulse.data.StockRepository
import app.stockpulse.ui.home.HomeViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun quotes(vm: HomeViewModel) = (vm.state.value as UiState.Success).value.map { it.quote.symbol }

    @Test
    fun homeLoadsQuotesForWatchlist() = runTest {
        val vm = HomeViewModel(StockRepository(FakeStockApi()), FakeWatchlistStore(listOf("AAPL", "MSFT")))
        advanceUntilIdle()
        assertEquals(listOf("AAPL", "MSFT"), quotes(vm))
    }

    @Test
    fun homeItemsCarrySparklineCloses() = runTest {
        val vm = HomeViewModel(StockRepository(FakeStockApi()), FakeWatchlistStore(listOf("AAPL")))
        advanceUntilIdle()
        assertEquals(60, (vm.state.value as UiState.Success).value.single().closes.size)
    }

    @Test
    fun homeOmitsCardsThatFailButKeepsTheRest() = runTest {
        val api = FakeStockApi().apply { unknown += "ZZZZ" }
        val vm = HomeViewModel(StockRepository(api), FakeWatchlistStore(listOf("AAPL", "ZZZZ", "MSFT")))
        advanceUntilIdle()
        assertEquals(listOf("AAPL", "MSFT"), quotes(vm))
    }

    @Test
    fun homeShowsErrorThenRecoversOnRefresh() = runTest {
        val api = FakeStockApi().apply { failure = IOException("offline") }
        val vm = HomeViewModel(StockRepository(api), FakeWatchlistStore(listOf("AAPL")))
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Error)
        api.failure = null
        vm.refresh()
        advanceUntilIdle()
        assertEquals(listOf("AAPL"), quotes(vm))
    }

    @Test
    fun emptyWatchlistIsEmptySuccessNotError() = runTest {
        val vm = HomeViewModel(StockRepository(FakeStockApi()), FakeWatchlistStore(emptyList()))
        advanceUntilIdle()
        assertEquals(emptyList<String>(), quotes(vm))
    }
}
