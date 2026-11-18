package com.mediadock.app.core.designsystem.components

import com.mediadock.app.core.designsystem.theme.*
import com.mediadock.app.core.designsystem.icons.HugeIcons

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.mediadock.app.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MediaDockTopBar(
    title: String,
    modifier: Modifier = Modifier,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < .5f
    TopAppBar(
        modifier = modifier,
        title = {
            if (title == "MediaDock" && !canNavigateBack) {
                Image(
                    painterResource(if (isDark) R.drawable.mediadock_wordmark_light else R.drawable.mediadock_wordmark_dark),
                    contentDescription = "MediaDock",
                    modifier = Modifier.width(176.dp).height(40.dp)
                )
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                if (!canNavigateBack) {
                    Image(
                        painterResource(if (isDark) R.drawable.mediadock_mark_light else R.drawable.mediadock_mark_dark),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(HugeIcons.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BrandDarkCanvas,
            scrolledContainerColor = BrandDarkSurface,
            titleContentColor = BrandTextPrimary,
            navigationIconContentColor = BrandTextPrimary,
            actionIconContentColor = BrandTextPrimary
        )
    )
}
