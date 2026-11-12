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
import com.mediadock.app.core.designsystem.components.MediaDockTopBar
import com.mediadock.app.core.designsystem.theme.MediaDockTheme
import com.mediadock.app.domain.usecase.GetActiveDownloadsUseCase
import com.mediadock.app.feature.navigation.MediaDockNavHost
import com.mediadock.app.feature.navigation.Screen
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

                val isRootScreen = currentRoute == Screen.Home.route ||
                        currentRoute == Screen.Downloads.route ||
                        currentRoute == Screen.History.route ||
                        currentRoute == Screen.Settings.route

                val screenTitle = when (currentRoute) {
                    Screen.Home.route -> "MediaDock"
                    Screen.Downloads.route -> "Downloads"
                    Screen.History.route -> "History"
                    Screen.Settings.route -> "Settings"
                    else -> "MediaDock"
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MediaDockTopBar(
                            title = screenTitle,
                            canNavigateBack = !isRootScreen,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    },
                    bottomBar = {
                        if (isRootScreen) {
                            MediaDockBottomBar(
                                currentRoute = currentRoute,
                                onNavigateToRoute = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                activeDownloadsCount = activeCount
                            )
                        }
                    }
                ) { innerPadding ->
                    MediaDockNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        sharedUrl = sharedUrl
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
