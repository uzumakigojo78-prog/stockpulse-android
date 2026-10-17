package app.stockpulse.ui

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.stockpulse.AppContainer
import app.stockpulse.data.POPULAR_STOCKS
import app.stockpulse.ui.detail.DetailScreen
import app.stockpulse.ui.detail.DetailViewModel
import app.stockpulse.ui.home.HomeScreen
import app.stockpulse.ui.home.HomeViewModel
import app.stockpulse.ui.search.SearchScreen
import app.stockpulse.ui.search.SearchViewModel
import app.stockpulse.ui.settings.SettingsScreen
import app.stockpulse.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.flowOf

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("stocks", "Stocks", Icons.AutoMirrored.Rounded.TrendingUp),
    Tab("favorites", "Favorites", Icons.Rounded.Star),
    Tab("settings", "Settings", Icons.Rounded.Settings),
)

@Composable
fun AppNav(container: AppContainer) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (TABS.any { it.route == route }) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo("stocks") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(nav, startDestination = "stocks", modifier = Modifier.padding(bottom = inner.calculateBottomPadding())) {
            composable("stocks") {
                val vm: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { HomeViewModel(container.repository, container.watchlist, flowOf(POPULAR_STOCKS)) }
                    },
                )
                HomeScreen(
                    vm, "StockPulse", "Popular stocks right now",
                    emptyText = "No stocks to show.",
                    onSearch = { nav.navigate("search") },
                    onOpen = { nav.navigate("detail/${Uri.encode(it)}") },
                )
            }
            composable("favorites") {
                val vm: HomeViewModel = viewModel(
                    factory = viewModelFactory { initializer { HomeViewModel(container.repository, container.watchlist) } },
                )
                HomeScreen(
                    vm, "Favorites", "Stocks you starred",
                    emptyText = "Nothing here yet. Tap the star on any stock to add it.",
                    onSearch = { nav.navigate("search") },
                    onOpen = { nav.navigate("detail/${Uri.encode(it)}") },
                )
            }
            composable("settings") {
                val vm: SettingsViewModel = viewModel(
                    factory = viewModelFactory { initializer { SettingsViewModel(container.settings, container.backup) } },
                )
                SettingsScreen(vm)
            }
            composable("search") {
                val vm: SearchViewModel = viewModel(
                    factory = viewModelFactory { initializer { SearchViewModel(container.repository) } },
                )
                SearchScreen(
                    vm,
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate("detail/${Uri.encode(it)}") },
                )
            }
            composable(
                "detail/{symbol}",
                arguments = listOf(navArgument("symbol") { type = NavType.StringType }),
            ) { entry ->
                val symbol = entry.arguments?.getString("symbol").orEmpty()
                val vm: DetailViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            DetailViewModel(container.repository, container.forecastEngine, container.watchlist, symbol)
                        }
                    },
                )
                DetailScreen(vm, symbol, onBack = { nav.popBackStack() })
            }
        }
    }
}
