package com.mediadock.app.feature.home

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.mediadock.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.components.BrandButton
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.*
import com.mediadock.app.domain.model.PlatformRegistry

@Composable fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToInspect: (String) -> Unit,
    onBrowse: (String) -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    onSavedLinks: () -> Unit = {},
    onQuickDownload: (String) -> Unit = onNavigateToInspect
) {
    val input by viewModel.urlInput.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val suggestion by viewModel.clipboardSuggestion.collectAsState()
    val focused = LocalWindowInfo.current.isWindowFocused
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(focused) { if (focused) { kotlinx.coroutines.delay(250); viewModel.refreshClipboardSuggestion() } }
    LaunchedEffect(input) {
        if (com.mediadock.app.core.datastore.WorkspaceRules.validUrl(input.trim())) {
            kotlinx.coroutines.delay(350)
            val url = input.trim(); focusManager.clearFocus(); keyboard?.hide(); viewModel.clearUrl(); onNavigateToInspect(url)
        }
    }
    HomeLanding(input, error, suggestion, viewModel::onUrlChange, viewModel::pasteFromClipboard,
        onSearch = {
            val value = input.trim()
            if (com.mediadock.app.core.datastore.WorkspaceRules.validUrl(value)) { viewModel.clearUrl(); onNavigateToInspect(value) }
            else if (value.isNotBlank()) onBrowse(value)
        },
        onSuggestion = { url -> viewModel.dismissClipboardSuggestion(); onQuickDownload(url) },
        onDismissSuggestion = viewModel::dismissClipboardSuggestion,
        onBrowse = onBrowse, onSavedLinks = onSavedLinks, onLibrary = onLibrary, modifier = modifier)
}

@Composable fun HomeLanding(
    input: String, error: String?, suggestion: String?, onInput: (String) -> Unit,
    onPaste: () -> Unit, onSearch: () -> Unit, onSuggestion: (String) -> Unit,
    onDismissSuggestion: () -> Unit, onBrowse: (String) -> Unit,
    onSavedLinks: () -> Unit, onLibrary: () -> Unit, modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize().background(BrandDarkCanvas)) {
        val contentHeight = maxHeight
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = contentHeight).padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.mediadock_wordmark_light),
                contentDescription = "MediaDock",
                modifier = Modifier.width(240.dp).height(52.dp)
            )
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(input, onInput, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search or paste a link", style = MaterialTheme.typography.bodyMedium, color = BrandTextMuted) },
                leadingIcon = { Icon(HugeIcons.Search, null, tint = BrandTextSecondary) },
                trailingIcon = { IconButton(onClick = onPaste) { Icon(HugeIcons.ContentPaste, "Paste link", tint = BrandCoralOrange) } },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = BrandDarkSurface,
                    focusedContainerColor = BrandDarkSurface,
                    unfocusedBorderColor = BrandDarkBorder,
                    focusedBorderColor = BrandCoralOrange,
                    focusedTextColor = BrandTextPrimary,
                    unfocusedTextColor = BrandTextPrimary,
                    cursorColor = BrandCoralOrange
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }))
            if (error != null) Text(error, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (input.isNotBlank()) BrandButton(onClick = onSearch, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Find media or search", fontWeight = FontWeight.Bold) }
            if (suggestion != null) {
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = BrandDarkSurface, border = androidx.compose.foundation.BorderStroke(1.dp, BrandDarkBorder)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(HugeIcons.ContentPaste, null, Modifier.size(20.dp), tint = BrandCoralOrange)
                            Text("Is this the link you copied?", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleSmall, color = BrandTextPrimary)
                            IconButton(onClick = onDismissSuggestion, Modifier.size(32.dp)) { Icon(HugeIcons.Close, "Dismiss copied link", Modifier.size(18.dp), tint = BrandTextSecondary) }
                        }
                        Text(suggestion, Modifier.padding(top = 6.dp, bottom = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = BrandTextSecondary)
                        BrandButton(onClick = { onSuggestion(suggestion) }, Modifier.fillMaxWidth()) { Icon(HugeIcons.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Download this link", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Favorite platforms", style = MaterialTheme.typography.titleSmall, color = BrandTextPrimary)
                TextButton(onClick = { onBrowse("") }) { Text("See all", color = BrandCoralOrange) }
            }
            PlatformRegistry.platforms.take(8).chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { platform -> Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onBrowse("https://${platform.domains.first()}") }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        PlatformIcon(platform.id)
                        Spacer(Modifier.height(8.dp))
                        Text(platform.name, style = MaterialTheme.typography.labelSmall, color = BrandTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onSavedLinks) { Icon(HugeIcons.FolderOpen, null, Modifier.size(18.dp), tint = BrandCoralOrange); Spacer(Modifier.width(6.dp)); Text("Saved links", color = BrandTextPrimary) }
                TextButton(onClick = onLibrary) { Icon(HugeIcons.PlayCircle, null, Modifier.size(18.dp), tint = BrandCoralOrange); Spacer(Modifier.width(6.dp)); Text("My collection", color = BrandTextPrimary) }
            }
        }
    }
}

@Composable
fun PlatformIcon(id: String, modifier: Modifier = Modifier) {
    val icon = when (id) {
        "youtube" -> HugeIcons.Youtube
        "instagram" -> HugeIcons.Instagram
        "tiktok" -> HugeIcons.Tiktok
        "facebook" -> HugeIcons.Facebook
        "pinterest" -> HugeIcons.Pinterest
        "soundcloud" -> HugeIcons.Soundcloud
        "reddit" -> HugeIcons.Reddit
        "twitter" -> HugeIcons.Twitter
        else -> HugeIcons.Language
    }
    Box(modifier.size(52.dp).background(BrandDarkSurface, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(28.dp), tint = BrandTextPrimary)
    }
}
