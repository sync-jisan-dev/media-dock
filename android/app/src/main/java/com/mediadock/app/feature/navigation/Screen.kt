package com.mediadock.app.feature.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object SavedLinks : Screen("saved_links")
    data object Explore : Screen("explore")
    data object Browser : Screen("browser/{url}") {
        fun createRoute(url: String) = "browser/${URLEncoder.encode(url, StandardCharsets.UTF_8.toString())}"
    }
    data object Downloads : Screen("downloads")
    data object History : Screen("history")
    data object Settings : Screen("settings")

    data object MediaInspect : Screen("media_inspect/{url}") {
        fun createRoute(url: String): String {
            val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            return "media_inspect/$encoded"
        }
    }
}
