package app.stockpulse

import android.content.Context
import app.stockpulse.data.BackupManager
import app.stockpulse.data.DataStoreWatchlistStore
import app.stockpulse.data.SettingsStore
import app.stockpulse.data.StockRepository
import app.stockpulse.data.WatchlistStore
import app.stockpulse.data.YahooApi
import app.stockpulse.domain.ForecastEngine
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

class AppContainer(context: Context) {
    val repository = StockRepository(YahooApi(HttpClient(CIO)))
    val watchlist: WatchlistStore = DataStoreWatchlistStore(context)
    val forecastEngine = ForecastEngine()
    val settings = SettingsStore(context)
    val backup = BackupManager(context.applicationContext, repository, watchlist, settings)
}
