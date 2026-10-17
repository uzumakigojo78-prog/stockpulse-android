package app.stockpulse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

private fun fixture(name: String) = object {}.javaClass.getResource("/$name")!!.readText()

class YahooParsingTest {
    @Test
    fun parsesBarsAndQuote() {
        val data = parseChart(fixture("chart_aapl.json"))
        assertEquals(3, data.bars.size)
        assertEquals(192.5, data.quote.price, 1e-9)
        assertEquals("AAPL", data.quote.symbol)
        assertEquals("Apple Inc.", data.quote.name)
        assertEquals(2.5, data.quote.change, 1e-9) // 192.5 - previous bar 190.0
    }

    @Test
    fun dropsNullClosesKeepingTimestampsAligned() {
        val data = parseChart(fixture("chart_with_nulls.json"))
        assertEquals(listOf(400.0, 404.0, 410.0), data.bars.map { it.close })
        assertEquals(listOf(1L, 3L, 5L), data.bars.map { it.epochSec })
    }

    @Test
    fun parsesSearchHitsKeepingEquitiesAndEtfsOnly() {
        val hits = parseSearch(fixture("search_apple.json"))
        assertEquals(listOf("AAPL", "APLE", "AAPU"), hits.map { it.symbol })
        assertEquals("NASDAQ", hits.first().exchange)
    }

    @Test
    fun errorPayloadThrowsNotFound() {
        assertThrows(StockException.NotFound::class.java) { parseChart(fixture("chart_not_found.json")) }
    }
}
