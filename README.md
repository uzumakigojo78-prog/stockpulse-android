# StockPulse

An Android app for browsing popular stocks, searching any ticker, and viewing a short-horizon statistical price forecast. Built with Kotlin, Jetpack Compose and the **Material 3 Expressive** theme.

## Features

- **Watchlist** of popular stocks (AAPL, MSFT, NVDA, TSLA, AMZN, GOOGL, META, NFLX, AMD, JPM) with price, day change and a sparkline. Pull to refresh; star or unstar any stock from its detail page.
- **Search** any stock, ETF or index with live suggestions.
- **Detail** page with a price chart (1D / 5D / 1M / 6M / 1Y) and a **forecast** for 1, 3 and 5 days: median price, 80% range and chance of closing higher.
- Material 3 Expressive: `MaterialExpressiveTheme` with the expressive motion scheme, dynamic color, shape-morphing toggle buttons, wavy progress indicators and the loading indicator.

## How the forecast works

Daily closes from the last year give a log-return volatility. The app simulates 1,500 random walks (seeded, so results are stable between refreshes) with a small, shrunk drift term and reports the median, the 10th to 90th percentile band and the share of paths ending above the last close. It needs at least 30 daily bars. Short-term price moves are close to unpredictable; treat this as an illustration of uncertainty, not a signal.

> Statistical estimates only, **not financial advice**.

## Data

Prices and search come from Yahoo Finance's public, unofficial endpoints with no API key. They may be delayed, rate limited, or change without notice.

## Build

Requires JDK 17 and the Android SDK (platform 36).

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. CI runs the same command and uploads the APK as a build artifact.

## Project layout

```
app/src/main/kotlin/app/stockpulse/
  data/     Yahoo API client, JSON parsing, repository, watchlist store
  domain/   models and the ForecastEngine (pure Kotlin)
  ui/       theme, navigation, Home / Search / Detail screens and view models
```

Note: Material 3 is pinned to `1.5.0-alpha10`. Later 1.5 builds require Android Gradle Plugin 9.1+ and compileSdk 37.
