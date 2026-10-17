package app.stockpulse.data

import app.stockpulse.domain.SearchHit
import java.io.IOException
import kotlinx.coroutines.CancellationException

enum class ChartRange(val label: String, val yahooRange: String, val interval: String) {
    D1("1D", "1d", "5m"),
    D5("5D", "5d", "15m"),
    M1("1M", "1mo", "1d"),
    M6("6M", "6mo", "1d"),
    Y1("1Y", "1y", "1d"),
}

class StockRepository(private val api: StockApi) {
    /** Latest price with day change (daily bars, so change is vs the previous close). */
    suspend fun quote(symbol: String): Result<ChartData> = call { parseChart(api.chart(symbol, "5d", "1d")) }

    suspend fun history(symbol: String, range: ChartRange): Result<ChartData> =
        call { parseChart(api.chart(symbol, range.yahooRange, range.interval)) }

    suspend fun search(query: String): Result<List<SearchHit>> {
        if (query.isBlank()) return Result.success(emptyList())
        return call { parseSearch(api.search(query.trim())) }
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: StockException) {
        Result.failure(e)
    } catch (e: IOException) {
        Result.failure(StockException.Network(cause = e))
    } catch (e: Exception) {
        Result.failure(StockException.Network("Unexpected response", e))
    }
}
