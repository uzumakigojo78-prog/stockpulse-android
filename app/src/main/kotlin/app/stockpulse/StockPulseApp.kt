package app.stockpulse

import android.app.Application

class StockPulseApp : Application() {
    val container by lazy { AppContainer(this) }
}
