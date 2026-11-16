package com.mediadock.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mediadock.app.R
import com.mediadock.app.core.designsystem.theme.DockAqua
import com.mediadock.app.core.designsystem.theme.DockInk

enum class BottomNavItem(val route: String, val titleRes: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, Icons.Rounded.Home),
    EXPLORE("explore", R.string.nav_explore, Icons.Rounded.Explore),
    DOWNLOADS("downloads", R.string.nav_downloads, Icons.Rounded.Download),
    HISTORY("history", R.string.nav_history, Icons.Rounded.FolderOpen),
    SETTINGS("settings", R.string.nav_settings, Icons.Rounded.PersonOutline)
}

@Composable
fun MediaDockBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    activeDownloadsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 5.dp,
            modifier = Modifier.shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = .14f))
        ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                BottomNavItem.entries.forEach { item ->
                    val selected = currentRoute == item.route
                    val scale by animateFloatAsState(
                        if (selected) 1.08f else 1f,
                        spring(dampingRatio = .72f, stiffness = 450f),
                        label = "navScale"
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { if (!selected) onNavigateToRoute(item.route) },
                        icon = {
                            val iconContent: @Composable () -> Unit = {
                                Icon(
                                    item.icon,
                                    contentDescription = stringResource(item.titleRes),
                                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                                )
                            }
                            if (item == BottomNavItem.DOWNLOADS && activeDownloadsCount > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = DockAqua, contentColor = DockInk) {
                                        Text(activeDownloadsCount.coerceAtMost(99).toString())
                                    }
                                }) { iconContent() }
                            } else iconContent()
                        },
                        label = { Text(stringResource(item.titleRes), style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
