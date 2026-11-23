package com.mediadock.app.feature.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.mediadock.app.feature.workspace.SavedLinksScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mediadock.app.core.designsystem.theme.MediaDockMotion
import com.mediadock.app.feature.downloads.DownloadsScreen
import com.mediadock.app.feature.downloads.DownloadsHub
import com.mediadock.app.feature.browser.BrowserScreen
import com.mediadock.app.feature.downloads.DownloadsViewModel
import com.mediadock.app.feature.history.HistoryScreen
import com.mediadock.app.feature.history.HistoryViewModel
import com.mediadock.app.feature.home.HomeScreen
import com.mediadock.app.feature.home.HomeViewModel
import com.mediadock.app.feature.media.MediaInspectViewModel
import com.mediadock.app.feature.settings.SettingsScreen
import com.mediadock.app.feature.settings.SettingsViewModel

@Composable
fun MediaDockNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    sharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {},
    onBrowserContentChanged: (Boolean) -> Unit = {}
) {
    val inspectViewModel: MediaInspectViewModel = hiltViewModel()
    val openInspection: (String) -> Unit = { url ->
        inspectViewModel.startInspection(url)
        navController.navigateToDownloads(DownloadsTab.QUEUE)
    }

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
                onLibrary = { navController.navigateToDownloads(DownloadsTab.FILES) },
                onNavigateToInspect = openInspection
            )
        }

        composable(Screen.Explore.route) {
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            BrowserScreen(
                initialUrl = "",
                onDownload = { url, title ->
                    downloadsViewModel.startDownload(url, title)
                    navController.navigateToDownloads(DownloadsTab.QUEUE)
                },
                onSavedLinks = { navController.navigate(Screen.SavedLinks.route) },
                onNavigateHome = { navController.navigateToTopLevel(Screen.Home.route) },
                onBrowserContentChanged = onBrowserContentChanged
            )
        }
        composable(Screen.Browser.route, arguments = listOf(navArgument("url") { type = NavType.StringType })) { entry ->
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            BrowserScreen(
                initialUrl = entry.arguments?.getString("url").orEmpty(),
                onDownload = { url, title ->
                    downloadsViewModel.startDownload(url, title)
                    navController.navigateToDownloads(DownloadsTab.QUEUE)
                },
                onSavedLinks = { navController.navigate(Screen.SavedLinks.route) },
                onNavigateHome = { navController.navigateToTopLevel(Screen.Home.route) },
                onBrowserContentChanged = onBrowserContentChanged
            )
        }

        composable(
            route = Screen.MediaInspect.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("quick") { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            // Resume an inspection destination restored from an older build.
            LaunchedEffect(entry) {
                val url = entry.arguments?.getString("url").orEmpty()
                val quick = entry.arguments?.getBoolean("quick") == true &&
                    entry.savedStateHandle.get<Boolean>("quickStarted") != true
                inspectViewModel.startInspection(url, quick)
                navController.popBackStack()
                navController.navigateToDownloads(DownloadsTab.QUEUE)
            }
        }

        composable(Screen.Downloads.route) { entry ->
            val state = entry.savedStateHandle
            val selectedTab by state.getStateFlow(DOWNLOADS_TAB_KEY, DownloadsTab.QUEUE.name).collectAsState()
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            val historyViewModel: HistoryViewModel = hiltViewModel()
            DownloadsHub(
                selectedTab = if (selectedTab == DownloadsTab.FILES.name) DownloadsTab.FILES else DownloadsTab.QUEUE,
                onTabSelected = { state[DOWNLOADS_TAB_KEY] = it.name }
            ) { tab, contentModifier ->
                when (tab) {
                    DownloadsTab.QUEUE -> DownloadsScreen(
                        viewModel = downloadsViewModel,
                        inspectionViewModel = inspectViewModel,
                        onNavigateToSettings = { navController.navigateToTopLevel(Screen.Settings.route) },
                        modifier = contentModifier
                    )
                    DownloadsTab.FILES -> HistoryScreen(
                        viewModel = historyViewModel,
                        modifier = contentModifier,
                        onBrowse = { navController.navigate(Screen.Browser.createRoute(it)) }
                    )
                }
            }
        }

        // Redirect a standalone My files destination restored from an older build.
        composable(Screen.History.route) {
            LaunchedEffect(Unit) {
                navController.popBackStack(Screen.History.route, inclusive = true)
                navController.navigateToDownloads(DownloadsTab.FILES)
            }
        }

        composable(Screen.SavedLinks.route) {
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            SavedLinksScreen(
                onBrowse = { navController.navigate(Screen.Browser.createRoute(it)) },
                onDownload = { url ->
                    downloadsViewModel.startDownload(url)
                    navController.navigateToDownloads(DownloadsTab.QUEUE)
                }
            )
        }
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = settingsViewModel, onSavedLinks = { navController.navigate(Screen.SavedLinks.route) })
        }
    }
}
