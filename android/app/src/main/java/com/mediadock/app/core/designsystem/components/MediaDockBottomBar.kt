package com.mediadock.app.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mediadock.app.R

enum class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Default.Home),
    DOWNLOADS("downloads", R.string.nav_downloads, Icons.Default.Download),
    HISTORY("history", R.string.nav_history, Icons.Default.History),
    SETTINGS("settings", R.string.nav_settings, Icons.Default.Settings)
}

@Composable
fun MediaDockBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    activeDownloadsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        BottomNavItem.entries.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { if (!isSelected) onNavigateToRoute(item.route) },
                icon = {
                    if (item == BottomNavItem.DOWNLOADS && activeDownloadsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(text = activeDownloadsCount.toString())
                                }
                            }
                        ) {
                            Icon(imageVector = item.icon, contentDescription = stringResource(item.titleRes))
                        }
                    } else {
                        Icon(imageVector = item.icon, contentDescription = stringResource(item.titleRes))
                    }
                },
                label = { Text(text = stringResource(item.titleRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
