package com.mediadock.app.feature.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mediadock.app.R
import com.mediadock.app.feature.navigation.DownloadsTab

@Composable
internal fun DownloadsHub(
    selectedTab: DownloadsTab,
    onTabSelected: (DownloadsTab) -> Unit,
    content: @Composable (DownloadsTab, Modifier) -> Unit
) {
    val tabState = rememberSaveableStateHolder()

    Column(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.nav_downloads),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
        )
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            DownloadsTab.entries.forEach { tab ->
                val title = when (tab) {
                    DownloadsTab.QUEUE -> R.string.downloads_tab_queue
                    DownloadsTab.FILES -> R.string.nav_history
                }
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(stringResource(title)) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(Modifier.weight(1f)) {
            tabState.SaveableStateProvider(selectedTab.name) {
                content(selectedTab, Modifier.fillMaxSize())
            }
        }
    }
}
