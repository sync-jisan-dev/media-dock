package com.mediadock.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mediadock.app.feature.browser.browserPageColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.datastore.ThemeMode
import com.mediadock.app.core.designsystem.components.MediaDockBottomBar
import com.mediadock.app.core.designsystem.theme.MediaDockTheme
import com.mediadock.app.domain.usecase.GetActiveDownloadsUseCase
import com.mediadock.app.feature.navigation.MediaDockNavHost
import com.mediadock.app.feature.navigation.Screen
import com.mediadock.app.feature.navigation.navigateToTopLevel
import com.mediadock.app.feature.navigation.topLevelRoutes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    @Inject
    lateinit var getActiveDownloadsUseCase: GetActiveDownloadsUseCase

    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        setContent {
            val prefs by preferencesDataStore.preferencesFlow.collectAsState(initial = null)
            val activeCount by getActiveDownloadsUseCase.getCount().collectAsState(initial = 0)

            val darkTheme = when (prefs?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            MediaDockTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                LaunchedEffect(sharedUrl) { if (!sharedUrl.isNullOrBlank()) navController.navigateToTopLevel(Screen.Home.route) }
                val isRootScreen = currentRoute in topLevelRoutes
                val pageColor = if (currentRoute == Screen.Home.route || currentRoute == Screen.Explore.route || currentRoute == Screen.Browser.route) browserPageColor() else androidx.compose.material3.MaterialTheme.colorScheme.background
                val view = LocalView.current
                SideEffect {
                    window.statusBarColor = pageColor.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = pageColor.luminance() > .5f
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = pageColor,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    bottomBar = {
                        if (isRootScreen) {
                            MediaDockBottomBar(
                                currentRoute = currentRoute,
                                onNavigateToRoute = { route ->
                                    navController.navigateToTopLevel(route)
                                },
                                activeDownloadsCount = activeCount
                            )
                        }
                    }
                ) { innerPadding ->
                    MediaDockNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        sharedUrl = sharedUrl,
                        onSharedUrlConsumed = { sharedUrl = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    sharedUrl = sharedText
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            val data = intent.dataString
            if (!data.isNullOrBlank()) {
                sharedUrl = data
            }
        }
    }
}
