package app.stockpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {
    @Test
    fun writesHeaderAndEscapesQuotesAndCommasInNames() {
        val q = Quote("AAPL", "Apple, \"Inc\"", 190.5, 1.0, 0.5)
        val lines = favoritesCsv(listOf("AAPL", "ZZZ"), mapOf("AAPL" to q), "T").lines()
        assertEquals("symbol,name,price,change_pct,exported_at", lines[0])
        assertEquals("AAPL,\"Apple, \"\"Inc\"\"\",190.5,0.5,T", lines[1])
        assertEquals("ZZZ,,,,T", lines[2]) // no quote fetched: blanks, symbol still saved
    }

    @Test
    fun roundTripsSymbolsAndIgnoresJunkAndDuplicates() {
        val csv = favoritesCsv(listOf("AAPL", "BRK-B", "^GSPC"), emptyMap(), "T")
        assertEquals(listOf("AAPL", "BRK-B", "^GSPC"), parseCsvSymbols(csv))
        assertEquals(listOf("MSFT"), parseCsvSymbols("symbol\n msft \nMSFT\n<script>\n\n"))
    }
}
