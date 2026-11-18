package com.mediadock.app.core.designsystem.components

import com.mediadock.app.core.designsystem.icons.HugeIcons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.theme.*
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
    val barColor = BrandDarkSurface
    val borderColor = BrandDarkBorder

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = barColor,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(color = borderColor)
            NavigationBar(
                modifier = Modifier.heightIn(min = 80.dp).padding(horizontal = 8.dp),
                containerColor = barColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
            ) {
                BottomNavItem.entries.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigateToRoute(item.route) },
                        icon = {
                            val iconContent: @Composable () -> Unit = {
                                Icon(item.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                            if (item == BottomNavItem.DOWNLOADS && activeDownloadsCount > 0) {
                                BadgedBox(badge = {
                                    Badge(
                                        containerColor = BrandCoralOrange,
                                        contentColor = BrandWhite
                                    ) {
                                        Text(if (activeDownloadsCount > 99) "99+" else activeDownloadsCount.toString())
                                    }
                                }) { iconContent() }
                            } else iconContent()
                        },
                        label = {
                            Text(
                                stringResource(item.titleRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandWhite,
                            selectedTextColor = BrandCoralOrange,
                            indicatorColor = BrandCoralOrange,
                            unselectedIconColor = BrandTextSecondary,
                            unselectedTextColor = BrandTextSecondary
                        )
                    )
                }
            }
        }
    }
}
