package app.stockpulse.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import java.io.IOException

/** Returns raw JSON; parsing lives in YahooDto.kt. */
interface StockApi {
    suspend fun chart(symbol: String, range: String, interval: String): String
    suspend fun search(query: String): String
}

class YahooApi(client: HttpClient) : StockApi {
    private val client = client.config {
        install(HttpTimeout) { requestTimeoutMillis = 10_000; connectTimeoutMillis = 10_000 }
    }

    override suspend fun chart(symbol: String, range: String, interval: String): String =
        fetch {
            url("$BASE/v8/finance/chart/${symbol.encodeURLPathPart()}")
            parameter("range", range)
            parameter("interval", interval)
        }

    override suspend fun search(query: String): String =
        fetch {
            url("$BASE/v1/finance/search")
            parameter("q", query)
            parameter("quotesCount", 10)
            parameter("newsCount", 0)
        }

    private suspend fun fetch(configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit): String {
        val response = try {
            client.get {
                header(HttpHeaders.UserAgent, USER_AGENT)
                configure()
            }
        } catch (e: IOException) {
            throw StockException.Network(cause = e)
        }
        return when {
            response.status == HttpStatusCode.NotFound -> throw StockException.NotFound()
            response.status == HttpStatusCode.TooManyRequests -> throw StockException.RateLimited()
            response.status.value !in 200..299 -> throw StockException.Network("HTTP ${response.status.value}")
            else -> response.bodyAsText()
        }
    }

    private companion object {
        const val BASE = "https://query1.finance.yahoo.com"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) StockPulse/1.0"
    }
}
