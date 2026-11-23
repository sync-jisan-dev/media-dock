package com.mediadock.app.feature.navigation

import androidx.navigation.NavHostController

internal val topLevelRoutes = setOf(
    Screen.Home.route, Screen.Explore.route, Screen.Downloads.route,
    Screen.Settings.route
)

internal enum class DownloadsTab { QUEUE, FILES }

internal const val DOWNLOADS_TAB_KEY = "downloads_tab"

internal fun NavHostController.navigateToDownloads(tab: DownloadsTab) {
    navigateToTopLevel(Screen.Downloads.route)
    currentBackStackEntry?.savedStateHandle?.set(DOWNLOADS_TAB_KEY, tab.name)
}

/** Shared by the bottom bar and shortcuts so tabs never create duplicate root screens. */
fun NavHostController.navigateToTopLevel(route: String) {
    if (currentDestination?.route == route) return

    if (route == Screen.Home.route) {
        // Restoring Home can restore the tab saved above it by popUpTo(saveState = true).
        // Return to the existing Home entry instead of restoring that outgoing tab.
        if (popBackStack(Screen.Home.route, inclusive = false, saveState = true)) return
    }

    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = route != Screen.Home.route
    }
}
