package app.stockpulse.domain

import java.util.Random
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** Volatility-scaled Monte Carlo forecast over daily closes. A statistical estimate, not advice. */
class ForecastEngine(
    private val paths: Int = 1500,
    private val seed: Long = 42,
) {
    fun forecast(bars: List<Bar>, horizons: List<Int> = listOf(1, 3, 5)): ForecastResult {
        val closes = bars.map { it.close }.filter { it > 0.0 && it.isFinite() }
        if (closes.size < MIN_BARS) return ForecastResult.InsufficientData

        val returns = closes.zipWithNext { a, b -> ln(b / a) }
        val rawMean = returns.average()
        val mu = rawMean * DRIFT_WEIGHT
        val variance = returns.sumOf { (it - rawMean) * (it - rawMean) } / (returns.size - 1)
        val sigma = sqrt(variance)
        val last = closes.last()
        val maxDays = horizons.max()

        val rng = Random(seed)
        // terminal[d][path] = price after d+1 days on that path
        val terminal = Array(maxDays) { DoubleArray(paths) }
        for (p in 0 until paths) {
            var logPrice = ln(last)
            for (d in 0 until maxDays) {
                logPrice += mu + sigma * rng.nextGaussian()
                terminal[d][p] = exp(logPrice)
            }
        }

        return ForecastResult.Ok(
            horizons.map { days ->
                val sorted = terminal[days - 1].sortedArray()
                HorizonForecast(
                    days = days,
                    median = percentile(sorted, 0.5),
                    low = percentile(sorted, 0.1),
                    high = percentile(sorted, 0.9),
                    probUp = sorted.count { it > last }.toDouble() / sorted.size,
                )
            },
        )
    }

    private fun percentile(sorted: DoubleArray, q: Double): Double =
        sorted[((sorted.size - 1) * q).toInt()]

    private companion object {
        const val MIN_BARS = 30
        const val DRIFT_WEIGHT = 0.2
    }
}
