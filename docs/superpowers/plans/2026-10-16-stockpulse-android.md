# StockPulse Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Material 3 Expressive Android app that shows popular stocks, searches any ticker, and displays a statistical price forecast.

**Architecture:** Single-module Compose app. `data` (Ktor client → Yahoo Finance public endpoints, repository) → `domain` (pure Kotlin models + seedable `ForecastEngine`) → `ui` (ViewModels exposing `StateFlow<UiState>`, Compose screens). Watchlist stored in DataStore.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Expressive (`androidx.compose.material3` expressive alpha, pinned in Task 1), Ktor client + kotlinx.serialization, DataStore, JUnit4 + kotlinx-coroutines-test, Gradle (Kotlin DSL, version catalog).

**Spec:** `docs/superpowers/specs/2026-10-16-stockpulse-android-design.md`

## Global Constraints

- Native Android, Kotlin + Jetpack Compose; theme is `MaterialExpressiveTheme` (Material 3 Expressive).
- minSdk 26; compile/targetSdk = current stable at Task 1.
- Package `app.stockpulse`; repo name `stockpulse-android`; existing Python `stockpulse` project is not touched.
- Data: Yahoo Finance public chart + search endpoints, no API key.
- Forecast: volatility-scaled Monte Carlo, horizons 1/3/5 days, 80% range, up/down probability; needs ≥ 30 daily bars else "not enough history".
- "Not financial advice" disclaimer visible on Detail.
- No accounts, alerts, portfolios, or ML (out of scope).
- Every download (JDK, Android SDK, Gradle deps beyond normal build) is confirmed with the user first.

## Review Focus

- Yahoo `close` arrays contain `null` entries (market holidays/gaps) → must be dropped, not crash or yield NaN.
- Flat price series (zero volatility) → forecast returns a degenerate band (low = median = high), probabilities finite, no NaN/∞.
- Fewer than 30 daily bars → `ForecastResult.InsufficientData`, UI shows the message.
- Symbols with special characters (`BRK-B`, `^GSPC`, `BTC-USD`) → URL-encoded correctly, still resolve.
- Unknown/delisted symbol or HTTP 404/429 or offline → `UiState.Error` with retry, never a crash.
- Blank/whitespace search query → no network call, empty suggestions.

---

### Task 1: Toolchain, Gradle scaffold, expressive theme

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `.gitignore`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml` (INTERNET permission), `app/src/main/kotlin/app/stockpulse/MainActivity.kt`, `app/src/main/kotlin/app/stockpulse/ui/theme/Theme.kt`
- Test: `app/src/test/kotlin/app/stockpulse/SmokeTest.kt`

**Interfaces:**
- Produces: `@Composable fun StockPulseTheme(content: @Composable () -> Unit)` wrapping `MaterialExpressiveTheme` (dynamic color on Android 12+, fallback expressive color scheme otherwise); Gradle wrapper; applicationId `app.stockpulse`.

- [ ] **Step 1: Ask the user to approve each download** (JDK 17 Temurin tarball, Android command-line tools zip, then SDK packages `platform-tools`, `platforms;android-<stable>`, `build-tools;<stable>` via `sdkmanager`), stating source and size. Install into `~/.local/jdk` and `~/Android/Sdk` (no sudo); export `JAVA_HOME`, `ANDROID_HOME`; accept SDK licenses only after the user agrees.
- [ ] **Step 2: Verify toolchain.** Run `java -version` and `sdkmanager --list_installed`. Expected: JDK 17 and the installed packages listed.
- [ ] **Step 3: Write `SmokeTest`** asserting `1 + 1 == 2` (proves the test task runs).
- [ ] **Step 4: Create the Gradle project.** Generate the wrapper (Gradle version from AGP compatibility table); in `libs.versions.toml` pin AGP, Kotlin, Compose BOM **alpha that includes `MaterialExpressiveTheme`** (look up the current alpha on Google Maven and record the exact version), Ktor, serialization, DataStore, lifecycle-viewmodel-compose, navigation-compose.
- [ ] **Step 5: Implement `StockPulseTheme`** in `Theme.kt` using `MaterialExpressiveTheme` with `MotionScheme.expressive()`; `MainActivity` shows a placeholder `Text` inside it.
- [ ] **Step 6: Run** `./gradlew testDebugUnitTest assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 7: Commit** `chore: scaffold Compose project with M3 Expressive theme`.

---

### Task 2: Domain models and ForecastEngine (TDD)

**Files:**
- Create: `app/src/main/kotlin/app/stockpulse/domain/Models.kt`, `.../domain/ForecastEngine.kt`
- Test: `app/src/test/kotlin/app/stockpulse/domain/ForecastEngineTest.kt`

**Interfaces:**
- Produces:
  - `data class Bar(val epochSec: Long, val close: Double)`
  - `data class HorizonForecast(val days: Int, val median: Double, val low: Double, val high: Double, val probUp: Double)` (low/high = 10th/90th percentile)
  - `sealed interface ForecastResult { data class Ok(val horizons: List<HorizonForecast>) : ForecastResult; data object InsufficientData : ForecastResult }`
  - `class ForecastEngine(private val paths: Int = 1500, private val seed: Long = 42)` with `fun forecast(bars: List<Bar>, horizons: List<Int> = listOf(1, 3, 5)): ForecastResult`
  - `data class Quote(val symbol: String, val name: String, val price: Double, val change: Double, val changePct: Double)`
  - `data class SearchHit(val symbol: String, val name: String, val exchange: String)`

- [ ] **Step 1: Write failing tests** in `ForecastEngineTest`: `insufficientDataUnder30Bars` (29 bars → `InsufficientData`); `bandOrdered` (random-walk 120 bars → for every horizon `low <= median <= high`); `probUpInUnitInterval`; `deterministicForSameSeed` (two engines, same seed → equal results); `flatSeriesIsDegenerate` (60 bars all 100.0 → low == median == high == 100.0, probUp finite); `risingSeriesFavoursUp` (strong uptrend with low noise → 1-day `probUp > 0.5`).
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests '*ForecastEngineTest'`. Expected: FAIL (class missing).
- [ ] **Step 3: Implement `ForecastEngine.forecast`.** Log returns from closes; mean μ shrunk toward 0 (weight 0.2); σ = sample std of returns (σ = 0 allowed). Simulate `paths` geometric walks to max horizon with `java.util.Random(seed)` Gaussian steps; per horizon take sorted terminal prices → median, 10th, 90th percentile, `probUp` = fraction above last close. Return `InsufficientData` if `bars.size < 30`.
- [ ] **Step 4: Run tests.** Expected: all PASS.
- [ ] **Step 5: Commit** `feat: add domain models and Monte Carlo ForecastEngine`.

---

### Task 3: Data layer (Yahoo API, parsing, repository)

**Files:**
- Create: `.../data/YahooDto.kt`, `.../data/YahooApi.kt`, `.../data/StockRepository.kt`
- Test: `app/src/test/kotlin/app/stockpulse/data/YahooParsingTest.kt`, `.../data/StockRepositoryTest.kt`, `app/src/test/resources/chart_aapl.json`, `chart_with_nulls.json`, `search_apple.json`

**Interfaces:**
- Consumes: `Bar`, `Quote`, `SearchHit` from Task 2.
- Produces:
  - `interface StockApi { suspend fun chart(symbol: String, range: String, interval: String): String; suspend fun search(query: String): String }` (returns raw JSON; `YahooApi(client: HttpClient) : StockApi` implements it against `https://query1.finance.yahoo.com/v8/finance/chart/{symbol}` and `/v1/finance/search`, setting a browser User-Agent, URL-encoding the symbol)
  - `fun parseChart(json: String): ChartData` where `data class ChartData(val quote: Quote, val bars: List<Bar>)`, throws `StockException.NotFound` for Yahoo error payloads
  - `fun parseSearch(json: String): List<SearchHit>` (equities/ETFs/indices only)
  - `sealed class StockException : Exception() { NotFound, Network, RateLimited }`
  - `class StockRepository(private val api: StockApi)` with `suspend fun quote(symbol: String): Result<ChartData>` (range `5d`/`1d`), `suspend fun history(symbol: String, range: ChartRange): Result<ChartData>`, `suspend fun search(query: String): Result<List<SearchHit>>` (blank → empty list, no API call), and `enum class ChartRange(val yahooRange: String, val interval: String) { D1("1d","5m"), D5("5d","15m"), M1("1mo","1d"), M6("6mo","1d"), Y1("1y","1d") }`
- [ ] **Step 1: Save fixtures** (small hand-written JSON mirroring Yahoo's shape: `chart.result[0].meta`, `timestamp[]`, `indicators.quote[0].close[]`, and `quotes[]` for search; `chart_with_nulls.json` has `null` entries in `close`).
- [ ] **Step 2: Write failing tests:** `parsesBarsAndQuote`; `dropsNullCloses` (nulls removed, timestamps stay aligned); `parsesSearchHits`; `errorPayloadThrowsNotFound`; `repositoryBlankQueryMakesNoCall` (fake `StockApi` counts calls); `repositoryMapsHttpFailuresToResult` (fake throwing → `Result.failure(StockException.Network)`); `specialSymbolsAreEncoded` (`^GSPC` → `%5EGSPC` in the request path, tested on `YahooApi` with Ktor `MockEngine`).
- [ ] **Step 3: Run tests.** Expected: FAIL.
- [ ] **Step 4: Implement** the DTOs with kotlinx.serialization (`ignoreUnknownKeys = true`), `parseChart`, `parseSearch`, `YahooApi` (Ktor `CIO` engine, 10 s timeouts), `StockRepository` mapping 404 → `NotFound`, 429 → `RateLimited`, IO errors → `Network`.
- [ ] **Step 5: Run tests.** Expected: PASS.
- [ ] **Step 6: Commit** `feat: add Yahoo Finance data layer`.

---

### Task 4: Watchlist store and ViewModels

**Files:**
- Create: `.../data/WatchlistStore.kt`, `.../ui/home/HomeViewModel.kt`, `.../ui/search/SearchViewModel.kt`, `.../ui/detail/DetailViewModel.kt`, `.../ui/UiState.kt`
- Test: `.../ui/home/HomeViewModelTest.kt`, `.../ui/search/SearchViewModelTest.kt`, `.../ui/detail/DetailViewModelTest.kt`

**Interfaces:**
- Consumes: `StockRepository`, `ForecastEngine`, `ChartRange`, `Quote`, `SearchHit`, `ForecastResult`.
- Produces:
  - `sealed interface UiState<out T> { data object Loading; data class Success<T>(val value: T); data class Error(val message: String) }`
  - `interface WatchlistStore { val symbols: Flow<List<String>>; suspend fun add(s: String); suspend fun remove(s: String) }`; `DataStoreWatchlistStore(context)` seeded with `AAPL, MSFT, NVDA, TSLA, AMZN, GOOGL, META, NFLX, AMD, JPM`
  - `HomeViewModel(repo, store)`: `val state: StateFlow<UiState<List<Quote>>>`, `fun refresh()`
  - `SearchViewModel(repo)`: `var query` + `fun onQueryChange(String)` (debounce 300 ms), `val hits: StateFlow<UiState<List<SearchHit>>>`
  - `DetailViewModel(repo, engine, symbol)`: `val state: StateFlow<UiState<DetailData>>`, `fun selectRange(ChartRange)`, `fun retry()`; `data class DetailData(val chart: ChartData, val range: ChartRange, val forecast: ForecastResult)` (forecast always computed from 1y daily bars)
- [ ] **Step 1: Write failing tests** with fake repo/store and `StandardTestDispatcher`: `homeLoadsQuotesForWatchlist`; `homeShowsErrorThenRecoversOnRefresh`; `searchDebouncesAndSkipsBlank`; `detailComputesForecastFromDailyBars`; `detailOfflineEmitsErrorAndRetryRecovers`.
- [ ] **Step 2: Run tests.** Expected: FAIL.
- [ ] **Step 3: Implement** the ViewModels (`viewModelScope`, `combine` of watchlist flow and fetched quotes; per-symbol failures omit that card rather than failing the screen unless all fail) and `DataStoreWatchlistStore` (preferences DataStore, comma-joined).
- [ ] **Step 4: Run tests.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add watchlist store and view models`.

---

### Task 5: Expressive UI (Home, Search, Detail) and navigation

**Files:**
- Create: `.../ui/home/HomeScreen.kt`, `.../ui/search/SearchScreen.kt`, `.../ui/detail/DetailScreen.kt`, `.../ui/detail/PriceChart.kt`, `.../ui/detail/ForecastCard.kt`, `.../ui/components/Sparkline.kt`, `.../ui/AppNav.kt`
- Modify: `MainActivity.kt` (set `AppNav` as content; manual DI via an `AppContainer`)
- Test: `app/src/test/kotlin/app/stockpulse/ui/ChartMathTest.kt` (pure helper `fun scaleToCanvas(values: List<Double>, width: Float, height: Float): List<Offset>`)

**Interfaces:**
- Consumes: all ViewModels and `UiState` from Task 4.
- Produces: `@Composable fun AppNav()` with routes `home`, `search`, `detail/{symbol}`.

- [ ] **Step 1: Write failing `ChartMathTest`:** `scalesMinToBottomMaxToTop`; `flatSeriesCentered` (all equal → y = height/2, no NaN); `singlePointDoesNotCrash`.
- [ ] **Step 2: Run, expect FAIL; implement `scaleToCanvas`; run, expect PASS.**
- [ ] **Step 3: Implement screens** with M3 Expressive components: Home = large flexible top app bar, a search entry (`SearchBar` in collapsed form), `LazyColumn`/grid of rounded-expressive-shape cards with `Sparkline`, green/red change chip, `PullToRefresh` with wavy progress; Search = `SearchBar` (expanded) with suggestion list, tapping navigates to detail and offers "add to watchlist"; Detail = price header, `ButtonGroup`/connected button group for `ChartRange`, canvas `PriceChart`, `ForecastCard` (table of 1/3/5-day median, 80% range, up-probability bar, "Not enough history" message for `InsufficientData`, disclaimer text "Statistical estimate, not financial advice."), `LoadingIndicator`/wavy progress and an error state with Retry button.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: If an emulator or device is available,** install and screenshot Home/Search/Detail; otherwise state in the final report that the UI was compiled but not run.
- [ ] **Step 6: Commit** `feat: add expressive Home, Search and Detail screens`.

---

### Task 6: CI, docs, GitHub repo

**Files:**
- Create: `.github/workflows/build.yml`, `README.md`, `LICENSE` (MIT, holder = GitHub username), update `.gitignore`

- [ ] **Step 1: Write `build.yml`:** on push/PR, JDK 17, `./gradlew testDebugUnitTest assembleDebug`, upload `app-debug.apk` artifact.
- [ ] **Step 2: Write `README.md`:** features, screenshots placeholder only if real screenshots exist, build instructions, data source caveat (unofficial Yahoo endpoints may change/rate-limit), forecast method, disclaimer.
- [ ] **Step 3: Run** `./gradlew testDebugUnitTest assembleDebug` one last time. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Ask the user to confirm** repo name `stockpulse-android`, visibility (public), and MIT license, then `gh repo create stockpulse-android --public --source . --push` (account `uzumakigojo78-prog`).
- [ ] **Step 5: Check the first CI run** with `gh run list`/`gh run watch`; report the result and repo URL.
