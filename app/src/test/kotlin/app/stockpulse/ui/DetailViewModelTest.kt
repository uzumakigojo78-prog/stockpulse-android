package app.stockpulse.ui

import app.stockpulse.data.ChartRange
import app.stockpulse.data.StockRepository
import app.stockpulse.domain.ForecastEngine
import app.stockpulse.domain.ForecastResult
import app.stockpulse.ui.detail.DetailViewModel
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
class DetailViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(api: FakeStockApi, symbol: String = "AAPL") =
        DetailViewModel(StockRepository(api), ForecastEngine(), symbol)

    @Test
    fun detailComputesForecastFromDailyBars() = runTest {
        val vm = vm(FakeStockApi())
        advanceUntilIdle()
        val data = (vm.state.value as UiState.Success).value
        assertTrue(data.forecast is ForecastResult.Ok)
        assertEquals(ChartRange.M6, data.range)
    }

    @Test
    fun detailShortHistoryReportsInsufficientData() = runTest {
        val vm = vm(FakeStockApi().apply { bars = 10 })
        advanceUntilIdle()
        val data = (vm.state.value as UiState.Success).value
        assertEquals(ForecastResult.InsufficientData, data.forecast)
    }

    @Test
    fun selectRangeKeepsForecastAndUpdatesRange() = runTest {
        val vm = vm(FakeStockApi())
        advanceUntilIdle()
        vm.selectRange(ChartRange.D5)
        advanceUntilIdle()
        assertEquals(ChartRange.D5, (vm.state.value as UiState.Success).value.range)
    }

    @Test
    fun detailOfflineEmitsErrorAndRetryRecovers() = runTest {
        val api = FakeStockApi().apply { failure = IOException("offline") }
        val vm = vm(api)
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Error)
        api.failure = null
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Success)
    }

    @Test
    fun detailUnknownSymbolIsErrorNotCrash() = runTest {
        val vm = vm(FakeStockApi().apply { unknown += "ZZZZ" }, "ZZZZ")
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Error)
    }
}
