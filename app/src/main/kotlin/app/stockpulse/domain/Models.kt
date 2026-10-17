package app.stockpulse.domain

data class Bar(val epochSec: Long, val close: Double)

data class HorizonForecast(
    val days: Int,
    val median: Double,
    val low: Double,
    val high: Double,
    val probUp: Double,
)

sealed interface ForecastResult {
    data class Ok(val horizons: List<HorizonForecast>) : ForecastResult
    data object InsufficientData : ForecastResult
}

data class Quote(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePct: Double,
)

data class SearchHit(val symbol: String, val name: String, val exchange: String)
