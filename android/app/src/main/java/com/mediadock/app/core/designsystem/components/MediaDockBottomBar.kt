package com.mediadock.app.core.designsystem.components

import com.mediadock.app.core.designsystem.icons.HugeIcons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

enum class BottomNavItem(val route: String, val titleRes: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, HugeIcons.Home),
    EXPLORE("explore", R.string.nav_explore, HugeIcons.Explore),
    DOWNLOADS("downloads", R.string.nav_downloads, HugeIcons.Download),
    HISTORY("history", R.string.nav_history, HugeIcons.FolderOpen),
    SETTINGS("settings", R.string.nav_settings, HugeIcons.PersonOutline)
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
                                    Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                                        Text(activeDownloadsCount.coerceAtMost(99).toString())
                                    }
                                }) { iconContent() }
                            } else iconContent()
                        },
                        label = { Text(stringResource(item.titleRes), style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
