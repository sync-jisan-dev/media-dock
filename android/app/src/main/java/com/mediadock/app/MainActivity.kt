package com.mediadock.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.mediadock.app.core.network.BackendConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.datastore.ThemeMode
import com.mediadock.app.core.designsystem.components.MediaDockBottomBar
import com.mediadock.app.core.designsystem.theme.MediaDockTheme
import com.mediadock.app.domain.usecase.GetActiveDownloadsUseCase
import com.mediadock.app.feature.navigation.DownloadsTab
import com.mediadock.app.feature.navigation.MediaDockNavHost
import com.mediadock.app.feature.navigation.Screen
import com.mediadock.app.feature.navigation.navigateToDownloads
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

    @Inject
    lateinit var backendConnection: BackendConnection

    private var backendWarmupJob: Job? = null

    private var sharedUrl by mutableStateOf<String?>(null)
    private var openQueueRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val permissionsToRequest = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 101)
        }

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
                var browserContentActive by remember { mutableStateOf(false) }

                LaunchedEffect(sharedUrl) { if (!sharedUrl.isNullOrBlank()) navController.navigateToTopLevel(Screen.Home.route) }
                LaunchedEffect(openQueueRequested) {
                    if (openQueueRequested) {
                        openQueueRequested = false
                        navController.navigateToDownloads(DownloadsTab.QUEUE)
                    }
                }
                LaunchedEffect(currentRoute) {
                    if (currentRoute != Screen.Explore.route && currentRoute != Screen.Browser.route) {
                        browserContentActive = false
                    }
                }
                val isRootScreen = currentRoute in topLevelRoutes
                // Keep the app navbar on Browse's start page. Once a real web page
                // is open, the browser becomes immersive and owns the full viewport.
                val showAppBottomBar = isRootScreen && !(currentRoute == Screen.Explore.route && browserContentActive)
                val pageColor = if (currentRoute == Screen.Home.route || currentRoute == Screen.Explore.route || currentRoute == Screen.Browser.route) browserPageColor() else androidx.compose.material3.MaterialTheme.colorScheme.background
                val view = LocalView.current
                SideEffect {
                    window.statusBarColor = pageColor.toArgb()
                    // Let the Scaffold's page color continue behind the system gesture handle.
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                    val insets = WindowCompat.getInsetsController(window, view)
                    val lightPage = pageColor.luminance() > .5f
                    insets.isAppearanceLightStatusBars = lightPage
                    insets.isAppearanceLightNavigationBars = lightPage
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = pageColor,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    bottomBar = {
                        if (showAppBottomBar) {
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
                        onSharedUrlConsumed = { sharedUrl = null },
                        onBrowserContentChanged = { browserContentActive = it }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        // Wake a sleeping Render instance as soon as the app becomes visible.
        // BackendConnection coalesces concurrent probes and caches readiness, so
        // link inspection can reuse this warm-up instead of starting another wait.
        backendWarmupJob?.cancel()
        backendWarmupJob = lifecycleScope.launch(Dispatchers.IO) {
            runCatching { backendConnection.awaitReady() }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("openQueue", false)) {
            openQueueRequested = true
        }
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
