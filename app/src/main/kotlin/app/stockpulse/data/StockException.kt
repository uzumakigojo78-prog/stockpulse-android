package app.stockpulse.data

sealed class StockException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotFound(message: String = "Symbol not found") : StockException(message)
    class RateLimited : StockException("Too many requests, try again shortly")
    class Network(message: String = "Network problem", cause: Throwable? = null) : StockException(message, cause)
}
