# StockPulse Android — Design

## Purpose
A personal/portfolio Android app to browse popular stocks, search any ticker, and view a statistical price forecast. Material 3 Expressive look and feel. Not financial advice; forecasts are labelled as statistical estimates.

## Decisions (from the user)
- Native Android app (Kotlin, Jetpack Compose, Material 3 Expressive).
- New, separate GitHub repo (`stockpulse-android`); the existing Python `stockpulse` project is left untouched.
- Install a local JDK + Android SDK so the app can be compiled and unit-tested here (each download confirmed with the user first).

## Assumptions (to be corrected if wrong)
- Free, keyless data: Yahoo Finance public chart + search endpoints (may be delayed ~15 min; unofficial, can change).
- Forecast is a volatility-scaled Monte Carlo (same idea as the existing `server.py`), not a promise of accuracy.
- Target: minSdk 26, compileSdk/targetSdk current stable.

## Scope
1. **Home** — watchlist of popular tickers (AAPL, MSFT, NVDA, TSLA, AMZN, GOOGL, META, plus a few more) as price cards: price, day change, sparkline.
2. **Search** — M3 `SearchBar` with live suggestions from Yahoo's search endpoint; selecting a result opens Detail. Any listed symbol is reachable.
3. **Detail** — price chart with range chips (1D/5D/1M/6M/1Y), key stats, and a **Forecast card**: median path, 80% range, up/down probability for 1/3/5-day horizons.
4. Disclaimer shown on Detail/Forecast.

Out of scope (YAGNI): accounts, push alerts, portfolios, persistence beyond a saved watchlist, ML models.

## Architecture
```
app/src/main/kotlin/.../
  data/    YahooApi (Ktor client), dto, StockRepository
  domain/  models, ForecastEngine (pure Kotlin, unit-tested)
  ui/      theme (MaterialExpressiveTheme), home/, search/, detail/ (Compose + ViewModel)
```
- Unidirectional data flow: Repository → ViewModel (`StateFlow<UiState>`) → Composables.
- `ForecastEngine` is pure and seedable so tests are deterministic.
- Watchlist persisted with DataStore.

## M3 Expressive
Use `MaterialExpressiveTheme` (material3 expressive APIs): expressive motion scheme, shape morphing / expressive shapes on cards and FABs, wavy progress indicator for loading, button group for range selection, large-title top app bar. Exact artifact versions pinned at plan time after checking availability.

## Error handling
Network/parse errors map to a `UiState.Error` with retry; empty search shows suggestions-empty state; rate-limit/HTTP errors surface a friendly message. Forecast needs ≥ 30 daily bars, otherwise shows "not enough history".

## Testing
- Unit: `ForecastEngine` (determinism, band ordering low ≤ median ≤ high, probability in [0,1]), DTO parsing against saved JSON fixtures, repository with a fake API.
- Build: `./gradlew testDebugUnitTest assembleDebug` locally; GitHub Actions workflow runs the same and uploads the debug APK.
- Not verified on a device/emulator unless one becomes available.

## Delivery
New public GitHub repo `stockpulse-android` under the authenticated account, with README (features, build, disclaimer), CI workflow, MIT license (confirm before creating).
