package com.mediadock.app.feature.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mediadock.app.core.designsystem.theme.MediaDockMotion
import com.mediadock.app.feature.downloads.DownloadsScreen
import com.mediadock.app.feature.browser.BrowserScreen
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
        modifier = modifier,
        enterTransition = { MediaDockMotion.ScreenEnter },
        exitTransition = { MediaDockMotion.ScreenExit },
        popEnterTransition = { MediaDockMotion.ScreenPopEnter },
        popExitTransition = { MediaDockMotion.ScreenPopExit }
    ) {
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            androidx.compose.runtime.LaunchedEffect(sharedUrl) { homeViewModel.handleSharedUrl(sharedUrl) }
            HomeScreen(
                viewModel = homeViewModel,
                onBrowse = { query ->
                    if (query.isBlank()) navController.navigate(Screen.Explore.route)
                    else navController.navigate(Screen.Browser.createRoute(query))
                },
                onLibrary = { navController.navigate(Screen.History.route) },
                onNavigateToInspect = { url ->
                    navController.navigate(Screen.MediaInspect.createRoute(url))
                }
            )
        }

        composable(Screen.Explore.route) {
            BrowserScreen(initialUrl = "", onDownload = { navController.navigate(Screen.MediaInspect.createRoute(it)) })
        }
        composable(Screen.Browser.route, arguments = listOf(navArgument("url") { type = NavType.StringType })) { entry ->
            BrowserScreen(initialUrl = entry.arguments?.getString("url").orEmpty(), onDownload = { navController.navigate(Screen.MediaInspect.createRoute(it)) })
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
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
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
