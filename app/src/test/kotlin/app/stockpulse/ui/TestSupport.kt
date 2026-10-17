package app.stockpulse.ui

import app.stockpulse.data.StockApi
import app.stockpulse.data.WatchlistStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

fun chartJson(symbol: String, bars: Int = 60): String {
    val stamps = (0 until bars).joinToString(",") { (1_700_000_000L + it * 86_400L).toString() }
    val closes = (0 until bars).joinToString(",") { "%.2f".format(100.0 + (it % 7) - (it % 3) * 0.5) }
    return """{"chart":{"result":[{"meta":{"symbol":"$symbol","shortName":"$symbol Inc.","regularMarketPrice":101.0},""" +
        """"timestamp":[$stamps],"indicators":{"quote":[{"close":[$closes]}]}}],"error":null}}"""
}

const val NOT_FOUND_JSON =
    """{"chart":{"result":null,"error":{"code":"Not Found","description":"No data found"}}}"""

class FakeStockApi : StockApi {
    var failure: Throwable? = null
    val unknown = mutableSetOf<String>()
    var bars = 60
    var searchCalls = 0

    override suspend fun chart(symbol: String, range: String, interval: String): String {
        failure?.let { throw it }
        return if (symbol in unknown) NOT_FOUND_JSON else chartJson(symbol, bars)
    }

    override suspend fun search(query: String): String {
        searchCalls++
        failure?.let { throw it }
        return """{"quotes":[{"symbol":"AAPL","shortname":"Apple","exchDisp":"NASDAQ","quoteType":"EQUITY"}]}"""
    }
}

class FakeWatchlistStore(initial: List<String>) : WatchlistStore {
    private val flow = MutableStateFlow(initial)
    override val symbols: Flow<List<String>> = flow
    override suspend fun add(symbol: String) { flow.value = flow.value + symbol }
    override suspend fun remove(symbol: String) { flow.value = flow.value - symbol }
}
