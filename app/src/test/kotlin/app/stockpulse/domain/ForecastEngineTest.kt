package app.stockpulse.domain

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastEngineTest {
    private fun walk(n: Int, drift: Double, vol: Double, seed: Long = 1): List<Bar> {
        val r = Random(seed)
        var p = 100.0
        return List(n) { i ->
            p *= Math.exp(drift + vol * r.nextGaussian())
            Bar(i * 86400L, p)
        }
    }

    private fun ok(r: ForecastResult) = (r as ForecastResult.Ok).horizons

    @Test
    fun insufficientDataUnder30Bars() {
        assertEquals(ForecastResult.InsufficientData, ForecastEngine().forecast(walk(29, 0.0, 0.01)))
    }

    @Test
    fun bandOrdered() {
        ok(ForecastEngine().forecast(walk(120, 0.0, 0.02))).forEach {
            assertTrue(it.low <= it.median && it.median <= it.high)
        }
    }

    @Test
    fun probUpInUnitInterval() {
        ok(ForecastEngine().forecast(walk(120, 0.0, 0.02))).forEach {
            assertTrue(it.probUp in 0.0..1.0)
        }
    }

    @Test
    fun deterministicForSameSeed() {
        val bars = walk(120, 0.0, 0.02)
        assertEquals(ForecastEngine(seed = 7).forecast(bars), ForecastEngine(seed = 7).forecast(bars))
    }

    @Test
    fun flatSeriesIsDegenerate() {
        val flat = List(60) { Bar(it * 86400L, 100.0) }
        ok(ForecastEngine().forecast(flat)).forEach {
            assertEquals(100.0, it.low, 1e-9)
            assertEquals(100.0, it.median, 1e-9)
            assertEquals(100.0, it.high, 1e-9)
            assertTrue(it.probUp.isFinite())
        }
    }

    @Test
    fun risingSeriesFavoursUp() {
        val rising = walk(120, 0.01, 0.001)
        assertTrue(ok(ForecastEngine().forecast(rising)).first { it.days == 1 }.probUp > 0.5)
    }

    @Test
    fun horizonsDefaultToOneThreeFive() {
        assertEquals(listOf(1, 3, 5), ok(ForecastEngine().forecast(walk(60, 0.0, 0.01))).map { it.days })
    }
}
