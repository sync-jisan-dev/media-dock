package com.mediadock.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediadock.app.R
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.BrandCoralOrangeDark
import com.mediadock.app.core.designsystem.theme.BrandWhite
import com.mediadock.app.core.designsystem.theme.CpdColor

enum class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val outlineIcon: ImageVector,
    val filledIcon: ImageVector
) {
    HOME("home", R.string.nav_home, HugeIcons.Home, HugeIcons.HomeFilled),
    EXPLORE("explore", R.string.nav_explore, HugeIcons.Search, HugeIcons.SearchFilled),
    DOWNLOADS("downloads", R.string.nav_downloads, HugeIcons.Download, HugeIcons.DownloadFilled),
    SETTINGS("settings", R.string.nav_settings, HugeIcons.Settings, HugeIcons.SettingsFilled)
}

@Composable
fun MediaDockBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    activeDownloadsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val isDark = CpdColor.isDark
    val dockBg = if (isDark) Color(0xFF222222) else Color(0xFFF1F3F5)
    val dockBorder = if (isDark) Color(0xFF3A3A3A) else Color(0xFFCCCCCC)
    val unselectedColor = if (isDark) Color(0xFFBBBBBB) else Color(0xFF494949)
    val activeLabelColor = if (isDark) Color(0xFFFF9271) else Color(0xFF9C3215)
    val activeContainer = if (isDark) Color(0xFF492D26) else Color(0xFFFFF5F1)
    val dockShape = RoundedCornerShape(24.dp)
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 8.dp + navBarInset)
            .clip(dockShape)
            .background(dockBg)
            .border(1.dp, dockBorder, dockShape)
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem.entries.forEach { item ->
            NavBarTabItem(
                item = item,
                selected = currentRoute == item.route,
                activeLabelColor = activeLabelColor,
                activeContainer = activeContainer,
                unselectedColor = unselectedColor,
                badgeCount = if (item == BottomNavItem.DOWNLOADS) activeDownloadsCount else 0,
                onClick = { onNavigateToRoute(item.route) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavBarTabItem(
    item: BottomNavItem,
    selected: Boolean,
    activeLabelColor: Color,
    activeContainer: Color,
    unselectedColor: Color,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val activeIconColor = Color(0xFFFE5222)
    val focusShape = RoundedCornerShape(18.dp)
    val indicatorColor by animateColorAsState(
        targetValue = when {
            pressed -> activeIconColor.copy(alpha = 0.15f)
            selected -> activeContainer
            else -> Color.Transparent
        },
        label = "navigationIndicator"
    )
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .then(if (focused) Modifier.border(1.5.dp, activeLabelColor, focusShape) else Modifier)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 48.dp)
                .heightIn(min = 30.dp)
                .background(indicatorColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(containerColor = BrandCoralOrangeDark, contentColor = BrandWhite) {
                            Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (selected) item.filledIcon else item.outlineIcon,
                    contentDescription = null,
                    tint = if (selected) activeIconColor else unselectedColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = stringResource(item.titleRes),
            color = if (selected) activeLabelColor else unselectedColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
