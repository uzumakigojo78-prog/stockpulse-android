package app.stockpulse.data

import app.stockpulse.domain.Bar
import app.stockpulse.domain.Quote
import app.stockpulse.domain.SearchHit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class ChartData(val quote: Quote, val bars: List<Bar>)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable private data class ChartEnvelope(val chart: ChartBody)
@Serializable private data class ChartBody(val result: List<ChartResult>? = null, val error: ChartError? = null)
@Serializable private data class ChartError(val code: String? = null, val description: String? = null)
@Serializable private data class ChartResult(
    val meta: Meta,
    val timestamp: List<Long>? = null,
    val indicators: Indicators? = null,
)
@Serializable private data class Meta(
    val symbol: String,
    val shortName: String? = null,
    val longName: String? = null,
    val regularMarketPrice: Double? = null,
    val chartPreviousClose: Double? = null,
)
@Serializable private data class Indicators(val quote: List<QuoteSeries> = emptyList())
@Serializable private data class QuoteSeries(val close: List<Double?> = emptyList())

@Serializable private data class SearchEnvelope(val quotes: List<SearchQuote> = emptyList())
@Serializable private data class SearchQuote(
    val symbol: String,
    val shortname: String? = null,
    val longname: String? = null,
    @SerialName("exchDisp") val exchange: String? = null,
    val quoteType: String? = null,
)

/**
 * Parses a Yahoo chart payload. Null closes (holidays/gaps) are dropped with their timestamps.
 * The quote's change is price minus the previous bar's close, so it is a day change only for
 * daily-interval payloads (use the repository's quote()).
 */
fun parseChart(raw: String): ChartData {
    val body = json.decodeFromString<ChartEnvelope>(raw).chart
    val result = body.result?.firstOrNull()
        ?: throw StockException.NotFound(body.error?.description ?: "Symbol not found")
    val stamps = result.timestamp.orEmpty()
    val closes = result.indicators?.quote?.firstOrNull()?.close.orEmpty()
    val bars = stamps.zip(closes).mapNotNull { (t, c) ->
        if (c != null && c.isFinite()) Bar(t, c) else null
    }
    if (bars.isEmpty()) throw StockException.NotFound("No price data")
    val price = result.meta.regularMarketPrice ?: bars.last().close
    val previous = if (bars.size >= 2) bars[bars.size - 2].close else result.meta.chartPreviousClose ?: price
    val change = price - previous
    return ChartData(
        quote = Quote(
            symbol = result.meta.symbol,
            name = result.meta.longName ?: result.meta.shortName ?: result.meta.symbol,
            price = price,
            change = change,
            changePct = if (previous != 0.0) change / previous * 100 else 0.0,
        ),
        bars = bars,
    )
}

private val searchableTypes = setOf("EQUITY", "ETF", "INDEX")

fun parseSearch(raw: String): List<SearchHit> =
    json.decodeFromString<SearchEnvelope>(raw).quotes
        .filter { it.quoteType in searchableTypes }
        .map { SearchHit(it.symbol, it.longname ?: it.shortname ?: it.symbol, it.exchange.orEmpty()) }
