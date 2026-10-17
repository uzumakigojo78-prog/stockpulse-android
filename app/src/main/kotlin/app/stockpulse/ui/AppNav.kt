package app.stockpulse.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.stockpulse.AppContainer
import app.stockpulse.ui.detail.DetailScreen
import app.stockpulse.ui.detail.DetailViewModel
import app.stockpulse.ui.home.HomeScreen
import app.stockpulse.ui.home.HomeViewModel
import app.stockpulse.ui.search.SearchScreen
import app.stockpulse.ui.search.SearchViewModel

@Composable
fun AppNav(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(
                factory = viewModelFactory { initializer { HomeViewModel(container.repository, container.watchlist) } },
            )
            HomeScreen(
                vm,
                onSearch = { nav.navigate("search") },
                onOpen = { nav.navigate("detail/${Uri.encode(it)}") },
            )
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
