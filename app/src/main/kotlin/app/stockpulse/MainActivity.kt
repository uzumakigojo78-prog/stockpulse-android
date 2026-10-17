package app.stockpulse

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.stockpulse.data.AppSettings
import app.stockpulse.ui.AppNav
import app.stockpulse.ui.theme.StockPulseTheme
import app.stockpulse.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as StockPulseApp).container
        setContent {
            val settings by container.settings.settings.collectAsState(AppSettings())
            val dark = isDarkTheme(settings)
            // Keep status/nav bar icons readable when the in-app theme differs from the system theme.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            StockPulseTheme(settings) { AppNav(container) }
        }
    }
}
