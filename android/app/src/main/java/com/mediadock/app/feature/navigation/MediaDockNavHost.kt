package com.mediadock.app.feature.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mediadock.app.feature.downloads.DownloadsScreen
import com.mediadock.app.feature.downloads.DownloadsViewModel
import com.mediadock.app.feature.history.HistoryScreen
import com.mediadock.app.feature.history.HistoryViewModel
import com.mediadock.app.feature.home.HomeScreen
import com.mediadock.app.feature.home.HomeViewModel
import com.mediadock.app.feature.media.MediaInspectScreen
import com.mediadock.app.feature.media.MediaInspectViewModel
import com.mediadock.app.feature.settings.SettingsScreen
import com.mediadock.app.feature.settings.SettingsViewModel

@Composable
fun MediaDockNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    sharedUrl: String? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            if (!sharedUrl.isNullOrBlank()) {
                homeViewModel.handleSharedUrl(sharedUrl)
            }
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToInspect = { url ->
                    navController.navigate(Screen.MediaInspect.createRoute(url))
                }
            )
        }

        composable(
            route = Screen.MediaInspect.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType }
            )
        ) {
            val inspectViewModel: MediaInspectViewModel = hiltViewModel()
            MediaInspectScreen(
                viewModel = inspectViewModel,
                onNavigateToDownloads = {
                    navController.navigate(Screen.Downloads.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Downloads.route) {
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            DownloadsScreen(
                viewModel = downloadsViewModel,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route)
                }
            )
        }

        composable(Screen.History.route) {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(viewModel = historyViewModel)
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = settingsViewModel)
        }
    }
}
