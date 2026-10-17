package app.stockpulse.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun fixture(name: String) = object {}.javaClass.getResource("/$name")!!.readText()

private class FakeApi(
    val chartJson: String = fixture("chart_aapl.json"),
    val searchJson: String = fixture("search_apple.json"),
    val fail: Throwable? = null,
) : StockApi {
    var searchCalls = 0
    override suspend fun chart(symbol: String, range: String, interval: String): String {
        fail?.let { throw it }
        return chartJson
    }
    override suspend fun search(query: String): String {
        searchCalls++
        fail?.let { throw it }
        return searchJson
    }
}

class StockRepositoryTest {
    @Test
    fun repositoryBlankQueryMakesNoCall() = runTest {
        val api = FakeApi()
        val result = StockRepository(api).search("   ")
        assertEquals(emptyList<Any>(), result.getOrThrow())
        assertEquals(0, api.searchCalls)
    }

    @Test
    fun repositoryMapsIoFailureToNetwork() = runTest {
        val result = StockRepository(FakeApi(fail = IOException("offline"))).quote("AAPL")
        assertTrue(result.exceptionOrNull() is StockException.Network)
    }

    @Test
    fun repositoryKeepsNotFoundFromPayload() = runTest {
        val result = StockRepository(FakeApi(chartJson = fixture("chart_not_found.json"))).quote("ZZZZ")
        assertTrue(result.exceptionOrNull() is StockException.NotFound)
    }

    @Test
    fun repositoryParsesQuote() = runTest {
        val data = StockRepository(FakeApi()).quote("AAPL").getOrThrow()
        assertEquals(192.5, data.quote.price, 1e-9)
    }

    @Test
    fun specialSymbolsAreEncoded() = runTest {
        var path = ""
        val engine = MockEngine { req ->
            path = req.url.encodedPath
            respond(fixture("chart_aapl.json"), HttpStatusCode.OK)
        }
        YahooApi(HttpClient(engine)).chart("^GSPC", "5d", "1d")
        assertEquals("/v8/finance/chart/%5EGSPC", path)
    }

    @Test
    fun httpStatusesMapToExceptions() = runTest {
        suspend fun call(status: HttpStatusCode): Throwable? = runCatching {
            YahooApi(HttpClient(MockEngine { respond("", status) })).chart("AAPL", "5d", "1d")
        }.exceptionOrNull()
        assertTrue(call(HttpStatusCode.NotFound) is StockException.NotFound)
        assertTrue(call(HttpStatusCode.TooManyRequests) is StockException.RateLimited)
        assertTrue(call(HttpStatusCode.InternalServerError) is StockException.Network)
    }
}
