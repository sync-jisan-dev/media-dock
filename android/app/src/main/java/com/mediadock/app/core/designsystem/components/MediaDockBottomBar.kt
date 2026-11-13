package com.mediadock.app.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mediadock.app.R
import com.mediadock.app.core.designsystem.theme.Chartreuse
import com.mediadock.app.core.designsystem.theme.DarkSpruce
import com.mediadock.app.core.designsystem.theme.RadioactiveGrass
import com.mediadock.app.core.designsystem.theme.SlimeLime

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

            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.18f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "BottomNavIconScale"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { if (!isSelected) onNavigateToRoute(item.route) },
                icon = {
                    val iconModifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                    if (item == BottomNavItem.DOWNLOADS && activeDownloadsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = SlimeLime,
                                    contentColor = DarkSpruce
                                ) {
                                    Text(text = activeDownloadsCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = stringResource(item.titleRes),
                                modifier = iconModifier
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(item.titleRes),
                            modifier = iconModifier
                        )
                    }
                },
                label = { Text(text = stringResource(item.titleRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkSpruce,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Chartreuse,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
