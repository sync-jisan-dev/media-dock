package com.mediadock.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.mediadock.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.components.AsciiHeroAnimation
import com.mediadock.app.core.designsystem.components.BrandButton
import com.mediadock.app.core.designsystem.components.CompoundSecondaryButton
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
    BoxWithConstraints(modifier.fillMaxSize().background(CpdColor.bgCanvasDefault)) {
        val contentHeight = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = contentHeight)
                .padding(horizontal = CpdSpace.space6x, vertical = CpdSpace.space5x),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Wordmark with Ambient Borderless ASCII Wave & Brand Color Tint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background interactive ASCII animation (subtle & fading into canvas all around)
                AsciiHeroAnimation(
                    modifier = Modifier.fillMaxSize(),
                    isDark = CpdColor.isDark,
                    interactive = true
                )
                // Wordmark tinted with MediaDock signature coral-orange brand color
                Image(
                    painter = painterResource(if (CpdColor.isDark) R.drawable.mediadock_wordmark_light else R.drawable.mediadock_wordmark_dark),
                    contentDescription = "MediaDock",
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(CpdColor.CoralRest),
                    modifier = Modifier
                        .width(230.dp)
                        .height(50.dp)
                )
            }

            Spacer(Modifier.height(CpdSpace.space6x))

            // Search / Paste Input styled with Compound Tokens
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CpdSpace.space14x),
                singleLine = true,
                placeholder = {
                    Text(
                        "Search or paste a media link",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CpdColor.textDisabled
                    )
                },
                leadingIcon = {
                    Icon(HugeIcons.Search, contentDescription = null, tint = CpdColor.textSecondary)
                },
                trailingIcon = {
                    IconButton(onClick = onPaste) {
                        Icon(HugeIcons.ContentPaste, contentDescription = "Paste link", tint = CpdColor.CoralRest)
                    }
                },
                shape = RoundedCornerShape(CpdRadius.radiusXl),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CpdColor.bgSubtleSecondary,
                    focusedContainerColor = CpdColor.bgSubtleSecondary,
                    unfocusedBorderColor = CpdColor.borderInteractiveSecondary,
                    focusedBorderColor = CpdColor.borderFocused,
                    focusedTextColor = CpdColor.textPrimary,
                    unfocusedTextColor = CpdColor.textPrimary,
                    cursorColor = CpdColor.CoralRest
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() })
            )

            if (error != null) {
                Text(
                    error,
                    Modifier.padding(top = CpdSpace.space2x),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Compound Tier 1 Action Button (The Premium CTA)
            if (input.isNotBlank()) {
                BrandButton(
                    onClick = onSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CpdSpace.space3x)
                        .height(CpdSpace.space13x)
                ) {
                    Icon(HugeIcons.Download, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(CpdSpace.space2x))
                    Text("Find media or search", fontWeight = FontWeight.SemiBold)
                }
            }

            // Copied Link Suggestion Card (Framer-motion spring transition)
            AnimatedVisibility(
                visible = suggestion != null,
                enter = MediaDockMotion.CardEnter,
                exit = fadeOut(tween(150)) + slideOutVertically { -20 }
            ) {
                if (suggestion != null) {
                    Column {
                        Spacer(Modifier.height(CpdSpace.space4x))
                        Surface(
                            shape = RoundedCornerShape(CpdRadius.radiusXl),
                            color = CpdColor.bgSubtleSecondary,
                            border = androidx.compose.foundation.BorderStroke(CpdBorderWidth.default, CpdColor.borderInteractiveSecondary)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(CpdSpace.space4x)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(HugeIcons.ContentPaste, null, Modifier.size(20.dp), tint = CpdColor.CoralRest)
                                    Text(
                                        "Is this the link you copied?",
                                        Modifier.weight(1f).padding(start = CpdSpace.space2x),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = CpdColor.textPrimary
                                    )
                                    IconButton(onClick = onDismissSuggestion, Modifier.size(32.dp)) {
                                        Icon(HugeIcons.Close, "Dismiss copied link", Modifier.size(18.dp), tint = CpdColor.textSecondary)
                                    }
                                }
                                Text(
                                    suggestion,
                                    Modifier.padding(top = CpdSpace.space15x, bottom = CpdSpace.space3x),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CpdColor.textSecondary
                                )
                                BrandButton(
                                    onClick = { onSuggestion(suggestion) },
                                    modifier = Modifier.fillMaxWidth().height(CpdSpace.space13x)
                                ) {
                                    Icon(HugeIcons.Download, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(CpdSpace.space2x))
                                    Text("Download this link", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(CpdSpace.space6x))

            // Platform Directory Section
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Favorite platforms", style = MaterialTheme.typography.titleSmall, color = CpdColor.textPrimary)
                TextButton(onClick = { onBrowse("") }) {
                    Text("See all", color = CpdColor.CoralRest)
                }
            }

            PlatformRegistry.platforms.take(8).chunked(4).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = CpdSpace.space1x),
                    horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)
                ) {
                    row.forEach { platform ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(CpdRadius.radiusLg))
                                .background(CpdColor.bgSubtleSecondary)
                                .border(CpdBorderWidth.hairline, CpdColor.borderInteractiveSecondary, RoundedCornerShape(CpdRadius.radiusLg))
                                .bouncyClickable { onBrowse("https://${platform.domains.first()}") }
                                .padding(vertical = CpdSpace.space3x),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PlatformIcon(platform.id)
                            Spacer(Modifier.height(CpdSpace.space15x))
                            Text(
                                platform.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = CpdColor.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(CpdSpace.space5x))

            // Secondary Shortcut Actions (Compound Secondary Tier)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CpdSpace.space3x)
            ) {
                CompoundSecondaryButton(
                    onClick = onSavedLinks,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(HugeIcons.FolderOpen, null, Modifier.size(18.dp), tint = CpdColor.CoralRest)
                    Spacer(Modifier.width(CpdSpace.space15x))
                    Text("Saved links", color = CpdColor.textPrimary)
                }
                CompoundSecondaryButton(
                    onClick = onLibrary,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(HugeIcons.PlayCircle, null, Modifier.size(18.dp), tint = CpdColor.CoralRest)
                    Spacer(Modifier.width(CpdSpace.space15x))
                    Text("My collection", color = CpdColor.textPrimary)
                }
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
    Box(
        modifier = modifier
            .size(44.dp)
            .background(CpdColor.bgSubtlePrimary, RoundedCornerShape(CpdRadius.radiusMd)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = CpdColor.textPrimary)
    }
}
