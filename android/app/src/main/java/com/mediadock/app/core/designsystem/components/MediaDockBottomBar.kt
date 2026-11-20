package com.mediadock.app.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediadock.app.R
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.*
import kotlinx.coroutines.launch

enum class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val outlineIcon: ImageVector,
    val filledIcon: ImageVector
) {
    HOME("home", R.string.nav_home, HugeIcons.Home, HugeIcons.HomeFilled),
    EXPLORE("explore", R.string.nav_explore, HugeIcons.Search, HugeIcons.SearchFilled),
    DOWNLOADS("downloads", R.string.nav_downloads, HugeIcons.Download, HugeIcons.DownloadFilled),
    HISTORY("history", R.string.nav_history, HugeIcons.FolderOpen, HugeIcons.FolderOpenFilled),
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

    // Dock surfaces
    val dockBg = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)
    val dockBorder = if (isDark) Color(0xFF2C2C32) else Color(0xFFE5E7EB)
    val unselectedColor = if (isDark) Color(0xFF71717A) else Color(0xFF8E95A2)

    // Our signature Brand Coral Orange
    val activeTabColor = BrandCoralOrange

    // Center FAB Brand Coral Orange palette & radiant aura
    val fabColors = listOf(BrandCoralOrangeLight, BrandCoralOrange, BrandCoralOrangeDark)
    val fabGlowColor = BrandCoralOrange

    val leftItems = listOf(BottomNavItem.HOME, BottomNavItem.EXPLORE)
    val rightItems = listOf(BottomNavItem.HISTORY, BottomNavItem.SETTINGS)
    val centerItem = BottomNavItem.DOWNLOADS
    val isCenterSelected = currentRoute == centerItem.route

    val coroutineScope = rememberCoroutineScope()
    val clickSquishScale = remember { Animatable(1f) }

    // System gesture bar clearance
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Dimensions
    val crestHeight = 22.dp
    val barBodyHeight = 64.dp
    val dockHeight = crestHeight + barBodyHeight
    val totalHeight = dockHeight + 8.dp + navBarInset

    // ── Dynamic Morph & Spring Animations ─────────────────────────────────────
    val animatedCrestHeight by animateDpAsState(
        targetValue = if (isCenterSelected) crestHeight else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "animatedCrestHeight"
    )

    // Center button vertical offset:
    // When selected: lifted into the crest at 2.dp
    // When unselected: rests flush with the dock row at 22.dp (the top of barBodyHeight)
    val fabOffsetY by animateDpAsState(
        targetValue = if (isCenterSelected) 2.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fabOffsetY"
    )

    val selectionScale by animateFloatAsState(
        targetValue = if (isCenterSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selectionScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isCenterSelected) (if (isDark) 0.85f else 0.65f) else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "glowAlpha"
    )

    val totalFabScale = selectionScale * clickSquishScale.value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ── Floating Dock Nav Bar Container ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 8.dp + navBarInset)
                .height(dockHeight)
        ) {
            // Hardware ambient shadow for the center dome when crest is active
            if (isCenterSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(72.dp)
                        .graphicsLayer { alpha = glowAlpha }
                        .shadow(
                            elevation = if (isDark) 16.dp else 10.dp,
                            shape = CircleShape,
                            ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.08f),
                            spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.04f)
                        )
                )
            }

            // Dock body shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(barBodyHeight)
                    .shadow(
                        elevation = if (isDark) 16.dp else 12.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.08f),
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.04f)
                    )
            )

            // Seamless vector canvas drawing the continuous curved silhouette & outer hairline border
            Canvas(modifier = Modifier.matchParentSize()) {
                val maxH = crestHeight.toPx()
                val currentH = animatedCrestHeight.toPx()
                val crestProgress = if (crestHeight.value > 0) (animatedCrestHeight / crestHeight).coerceIn(0f, 1f) else 0f
                val w = 48.dp.toPx() * crestProgress
                val r = 28.dp.toPx()
                val cx = size.width / 2f
                val bottom = size.height
                val right = size.width

                val path = Path().apply {
                    moveTo(0f, maxH + r)
                    quadraticBezierTo(0f, maxH, r, maxH)
                    lineTo(cx - w, maxH)
                    cubicTo(
                        cx - w * 0.52f, maxH,
                        cx - w * 0.45f, maxH - currentH,
                        cx, maxH - currentH
                    )
                    cubicTo(
                        cx + w * 0.45f, maxH - currentH,
                        cx + w * 0.52f, maxH,
                        cx + w, maxH
                    )
                    lineTo(right - r, maxH)
                    quadraticBezierTo(right, maxH, right, maxH + r)
                    lineTo(right, bottom - r)
                    quadraticBezierTo(right, bottom, right - r, bottom)
                    lineTo(r, bottom)
                    quadraticBezierTo(0f, bottom, 0f, bottom - r)
                    close()
                }

                // Fill unified background
                drawPath(path = path, color = dockBg)

                // Stroke seamless outer perimeter
                drawPath(
                    path = path,
                    color = dockBorder,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Row containing the side navigation items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(barBodyHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Left 2 items: Home, Browse
                leftItems.forEach { item ->
                    NavBarTabItem(
                        item = item,
                        selected = currentRoute == item.route,
                        activeColor = activeTabColor,
                        unselectedColor = unselectedColor,
                        onClick = { onNavigateToRoute(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Center space reserved for the Queue button
                Spacer(modifier = Modifier.weight(1f))

                // Right 2 items: My files, Settings
                rightItems.forEach { item ->
                    NavBarTabItem(
                        item = item,
                        selected = currentRoute == item.route,
                        activeColor = activeTabColor,
                        unselectedColor = unselectedColor,
                        onClick = { onNavigateToRoute(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Center Action Button (Queue) ────────────────────────────────────
        // - When unselected: Background circle is completely transparent. The outline icon
        //   and "Queue" label rest in line with the other tabs.
        // - When selected: Springs upward into the curved crest, reveals the radiant
        //   Brand Coral Orange gradient circle and glowing aura, and REMOVES the text label
        //   so ONLY the white icon stays in the middle of the circle!
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = fabOffsetY)
                .size(width = 64.dp, height = if (isCenterSelected) 54.dp else barBodyHeight)
                .scale(totalFabScale),
            contentAlignment = Alignment.Center
        ) {
            // Soft glowing drop shadow / aura behind the circular button (visible only when selected)
            if (isCenterSelected) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .graphicsLayer { alpha = glowAlpha }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    fabGlowColor.copy(alpha = if (isDark) 0.65f else 0.45f),
                                    fabGlowColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Action Button Surface
            Box(
                modifier = if (isCenterSelected) {
                    Modifier
                        .size(54.dp)
                        .shadow(
                            elevation = (16 * selectionScale).dp,
                            shape = CircleShape,
                            ambientColor = if (isDark) fabGlowColor.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.15f),
                            spotColor = fabGlowColor.copy(alpha = 0.7f)
                        )
                        .clip(CircleShape)
                        .background(brush = Brush.verticalGradient(fabColors))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                } else {
                    Modifier
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.Transparent)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    coroutineScope.launch {
                        clickSquishScale.animateTo(0.85f, animationSpec = tween(60))
                        clickSquishScale.animateTo(
                            1.15f,
                            animationSpec = spring(
                                dampingRatio = 0.42f,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                        clickSquishScale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    onNavigateToRoute(centerItem.route)
                }
                .semantics {
                    contentDescription = "Queue"
                },
                contentAlignment = Alignment.Center
            ) {
                if (isCenterSelected) {
                    // Selected state: ONLY the icon in the middle of the circle
                    BadgedBox(
                        badge = {
                            if (activeDownloadsCount > 0) {
                                Badge(
                                    containerColor = BrandWhite,
                                    contentColor = fabGlowColor
                                ) {
                                    Text(
                                        text = if (activeDownloadsCount > 99) "99+" else activeDownloadsCount.toString(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = centerItem.filledIcon,
                            contentDescription = stringResource(centerItem.titleRes),
                            tint = BrandWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    // Unselected state: Transparent background, outline icon + "Queue" text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeDownloadsCount > 0) {
                                    Badge(
                                        containerColor = fabGlowColor,
                                        contentColor = BrandWhite
                                    ) {
                                        Text(
                                            text = if (activeDownloadsCount > 99) "99+" else activeDownloadsCount.toString(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = centerItem.outlineIcon,
                                contentDescription = stringResource(centerItem.titleRes),
                                tint = unselectedColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(centerItem.titleRes),
                            color = unselectedColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Side navigation tab item.
 * - Outline icon when unselected, filled icon when selected.
 * - Always shows clear text label with Brand Coral Orange when active.
 */
@Composable
private fun NavBarTabItem(
    item: BottomNavItem,
    selected: Boolean,
    activeColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale_${item.name}"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = item.route
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.filledIcon else item.outlineIcon,
            contentDescription = stringResource(item.titleRes),
            tint = if (selected) activeColor else unselectedColor,
            modifier = Modifier
                .size(24.dp)
                .scale(iconScale)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = stringResource(item.titleRes),
            color = if (selected) activeColor else unselectedColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
