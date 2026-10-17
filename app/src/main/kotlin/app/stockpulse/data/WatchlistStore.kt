package app.stockpulse.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WatchlistStore {
    val symbols: Flow<List<String>>
    suspend fun add(symbol: String)
    suspend fun remove(symbol: String)
}

val DEFAULT_WATCHLIST = listOf("AAPL", "MSFT", "NVDA", "TSLA", "AMZN", "GOOGL", "META", "NFLX", "AMD", "JPM")

private val Context.watchlistDataStore by preferencesDataStore("watchlist")
private val SYMBOLS_KEY = stringPreferencesKey("symbols")

class DataStoreWatchlistStore(context: Context) : WatchlistStore {
    private val store = context.applicationContext.watchlistDataStore

    override val symbols: Flow<List<String>> = store.data.map { prefs ->
        prefs[SYMBOLS_KEY]?.split(',')?.filter { it.isNotBlank() } ?: DEFAULT_WATCHLIST
    }

    override suspend fun add(symbol: String) = update { current ->
        val s = symbol.trim().uppercase()
        if (s.isEmpty() || s in current) current else current + s
    }

    override suspend fun remove(symbol: String) = update { current -> current - symbol.trim().uppercase() }

    private suspend fun update(transform: (List<String>) -> List<String>) {
        store.edit { prefs ->
            val current = prefs[SYMBOLS_KEY]?.split(',')?.filter { it.isNotBlank() } ?: DEFAULT_WATCHLIST
            prefs[SYMBOLS_KEY] = transform(current).joinToString(",")
        }
    }
}
