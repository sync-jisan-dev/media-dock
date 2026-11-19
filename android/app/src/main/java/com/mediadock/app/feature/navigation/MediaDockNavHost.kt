package com.mediadock.app.feature.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.mediadock.app.feature.workspace.SavedLinksScreen
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
    sharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes)
                fadeIn(tween(150)) else MediaDockMotion.ScreenEnter
        },
        exitTransition = {
            if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes)
                fadeOut(tween(90)) else MediaDockMotion.ScreenExit
        },
        popEnterTransition = {
            if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes)
                fadeIn(tween(150)) else MediaDockMotion.ScreenPopEnter
        },
        popExitTransition = {
            if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes)
                fadeOut(tween(90)) else MediaDockMotion.ScreenPopExit
        }
    ) {
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            androidx.compose.runtime.LaunchedEffect(sharedUrl) { if (sharedUrl != null) { homeViewModel.handleSharedUrl(sharedUrl); onSharedUrlConsumed() } }
            HomeScreen(
                viewModel = homeViewModel,
                onBrowse = { query ->
                    if (query.isBlank()) navController.navigateToTopLevel(Screen.Explore.route)
                    else navController.navigate(Screen.Browser.createRoute(query))
                },
                onSavedLinks = { navController.navigate(Screen.SavedLinks.route) },
                onQuickDownload = { url ->
                    homeViewModel.startDownload(url)
                    navController.navigateToTopLevel(Screen.Downloads.route)
                },
                onLibrary = { navController.navigateToTopLevel(Screen.History.route) },
                onNavigateToInspect = { url ->
                    homeViewModel.startDownload(url)
                    navController.navigateToTopLevel(Screen.Downloads.route)
                }
            )
        }

        composable(Screen.Explore.route) {
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            BrowserScreen(
                initialUrl = "",
                onDownload = { url ->
                    downloadsViewModel.startDownload(url)
                    navController.navigateToTopLevel(Screen.Downloads.route)
                },
                onSavedLinks = { navController.navigate(Screen.SavedLinks.route) }
            )
        }
        composable(Screen.Browser.route, arguments = listOf(navArgument("url") { type = NavType.StringType })) { entry ->
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            BrowserScreen(
                initialUrl = entry.arguments?.getString("url").orEmpty(),
                onDownload = { url ->
                    downloadsViewModel.startDownload(url)
                    navController.navigateToTopLevel(Screen.Downloads.route)
                },
                onSavedLinks = { navController.navigate(Screen.SavedLinks.route) }
            )
        }

        composable(
            route = Screen.MediaInspect.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("quick") { type = NavType.BoolType; defaultValue = false }
            )
        ) {
            val inspectViewModel: MediaInspectViewModel = hiltViewModel()
            MediaInspectScreen(
                viewModel = inspectViewModel,
                onNavigateToDownloads = {
                    navController.navigateToTopLevel(Screen.Downloads.route)
                },
                onNavigateToSettings = {
                    navController.navigateToTopLevel(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Downloads.route) {
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            DownloadsScreen(
                viewModel = downloadsViewModel,
                onNavigateHome = {
                    navController.navigateToTopLevel(Screen.Home.route)
                }
            )
        }

        composable(Screen.History.route) {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(viewModel = historyViewModel, onBrowse = { navController.navigate(Screen.Browser.createRoute(it)) })
        }

        composable(Screen.SavedLinks.route) {
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            SavedLinksScreen(
                onBrowse = { navController.navigate(Screen.Browser.createRoute(it)) },
                onDownload = { url ->
                    downloadsViewModel.startDownload(url)
                    navController.navigateToTopLevel(Screen.Downloads.route)
                }
            )
        }
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = settingsViewModel, onSavedLinks = { navController.navigate(Screen.SavedLinks.route) })
        }
    }
}
