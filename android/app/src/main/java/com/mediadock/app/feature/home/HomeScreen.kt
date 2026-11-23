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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediadock.app.core.ai.AiDuplicateReport
import com.mediadock.app.core.ai.AiLinkIntel
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
    val preset by viewModel.selectedPreset.collectAsState()
    val aiDetection by viewModel.aiDetection.collectAsState()
    val duplicateWarning by viewModel.duplicateWarning.collectAsState()

    var batchDialogOpen by remember { mutableStateOf(false) }
    var batchText by remember { mutableStateOf("") }
    var utilityDialogTitle by remember { mutableStateOf<String?>(null) }
    var utilityDialogPrompt by remember { mutableStateOf("") }

    if (batchDialogOpen) {
        val extractedBatch = com.mediadock.app.core.datastore.WorkspaceRules.extractLinks(batchText)
        AlertDialog(
            onDismissRequest = { batchDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(HugeIcons.Download, null, tint = CpdColor.CoralRest, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Batch Multi-Download", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste up to 50 URLs (one per line or mixed in text). All valid media links will be queued.", style = MaterialTheme.typography.bodySmall, color = CpdColor.textSecondary)
                    OutlinedTextField(
                        value = batchText,
                        onValueChange = { batchText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("https://youtube.com/...\nhttps://instagram.com/...\nhttps://tiktok.com/...") },
                        shape = RoundedCornerShape(CpdRadius.radiusMd)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(CpdRadius.radiusFull), color = CpdColor.CoralRest.copy(alpha = 0.15f)) {
                            Text("${extractedBatch.size} links found", Modifier.padding(horizontal = CpdSpace.space2x, vertical = CpdSpace.space1x), style = MaterialTheme.typography.labelSmall, color = CpdColor.CoralRest)
                        }
                        TextButton(onClick = { viewModel.pasteFromClipboard() }) {
                            Text("Paste clipboard", color = CpdColor.CoralRest)
                        }
                    }
                }
            },
            confirmButton = {
                BrandButton(
                    onClick = {
                        if (extractedBatch.isNotEmpty()) {
                            viewModel.startBatchDownload(extractedBatch, preset)
                            batchDialogOpen = false
                            batchText = ""
                            onQuickDownload(extractedBatch.first())
                        }
                    },
                    enabled = extractedBatch.isNotEmpty()
                ) {
                    Text("Download all (${extractedBatch.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { batchDialogOpen = false }) { Text("Cancel") }
            }
        )
    }

    if (utilityDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { utilityDialogTitle = null },
            title = { Text(utilityDialogTitle.orEmpty()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter target media URL for ${utilityDialogTitle?.lowercase()}:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = utilityDialogPrompt,
                        onValueChange = { utilityDialogPrompt = it },
                        modifier = Modifier.fillMaxWidth().height(CpdSize.inputHeight),
                        singleLine = true,
                        placeholder = { Text("https://...") },
                        shape = RoundedCornerShape(CpdRadius.radiusMd)
                    )
                }
            },
            confirmButton = {
                BrandButton(
                    onClick = {
                        val clean = utilityDialogPrompt.trim()
                        if (com.mediadock.app.core.datastore.WorkspaceRules.validUrl(clean)) {
                            utilityDialogTitle = null
                            utilityDialogPrompt = ""
                            onQuickDownload(clean)
                        }
                    },
                    enabled = com.mediadock.app.core.datastore.WorkspaceRules.validUrl(utilityDialogPrompt.trim())
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { utilityDialogTitle = null }) { Text("Cancel") }
            }
        )
    }

    HomeLanding(
        input = input,
        error = error,
        aiDetection = aiDetection,
        duplicateWarning = duplicateWarning,
        onInput = viewModel::onUrlChange,
        onClearInput = viewModel::clearUrl,
        onPaste = viewModel::pasteFromClipboard,
        onSearch = {
            val value = input.trim()
            if (com.mediadock.app.core.datastore.WorkspaceRules.validUrl(value)) {
                viewModel.clearUrl()
                onQuickDownload(value)
            } else if (value.isNotBlank()) {
                viewModel.addRecentSearch(value)
                onBrowse(value)
            }
        },
        onQuickAiDownload = { url ->
            viewModel.clearUrl()
            onQuickDownload(url)
        },
        onBrowse = onBrowse,
        onSavedLinks = onSavedLinks,
        onLibrary = onLibrary,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun HomeLanding(
    input: String,
    error: String?,
    aiDetection: AiLinkIntel?,
    duplicateWarning: AiDuplicateReport?,
    onInput: (String) -> Unit,
    onClearInput: () -> Unit,
    onPaste: () -> Unit,
    onSearch: () -> Unit,
    onQuickAiDownload: (String) -> Unit,
    onBrowse: (String) -> Unit,
    onSavedLinks: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var platformCategory by remember { mutableStateOf("All") }

    BoxWithConstraints(modifier.fillMaxSize().background(CpdColor.bgCanvasDefault)) {
        val contentHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = contentHeight)
                    .padding(horizontal = CpdSpace.space6x, vertical = CpdSpace.space4x),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            // Hero Wordmark with Ambient Borderless ASCII Wave
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                AsciiHeroAnimation(
                    modifier = Modifier.fillMaxSize(),
                    isDark = CpdColor.isDark,
                    interactive = true,
                    opacity = 0.6f
                )
                Image(
                    painter = painterResource(if (CpdColor.isDark) R.drawable.mediadock_wordmark_dark else R.drawable.mediadock_wordmark_light),
                    contentDescription = "MediaDock",
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(CpdColor.CoralRest),
                    modifier = Modifier
                        .width(230.dp)
                        .height(50.dp)
                )
            }

            Spacer(Modifier.height(CpdSpace.space2x))

            // Search / Paste Input
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier
                    .fillMaxWidth()
                    // Long URLs wrap into a readable preview instead of scrolling the
                    // beginning off-screen behind the trailing actions.
                    .height(if (input.length > 36) 96.dp else CpdSize.inputHeight),
                singleLine = false,
                minLines = 1,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = CpdColor.textPrimary,
                    lineHeight = 20.sp
                ),
                placeholder = {
                    Text(
                        "Search or paste media link",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CpdColor.textDisabled
                    )
                },
                leadingIcon = {
                    Icon(HugeIcons.Search, contentDescription = null, modifier = Modifier.size(CpdSize.iconMd), tint = CpdColor.textSecondary)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (input.isNotBlank()) {
                            IconButton(onClick = onClearInput) {
                                Icon(HugeIcons.Close, contentDescription = "Clear", tint = CpdColor.textSecondary)
                            }
                        }
                        IconButton(onClick = onPaste) {
                            Icon(HugeIcons.ContentPaste, contentDescription = "Paste link", tint = CpdColor.CoralRest)
                        }
                    }
                },
                shape = RoundedCornerShape(CpdRadius.radiusMd),
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

            // Real-time AI Brain Link Intelligence & Duplicate Guard
            AnimatedVisibility(
                visible = aiDetection != null,
                enter = fadeIn(tween(180)) + slideInVertically { -12 },
                exit = fadeOut(tween(120)) + slideOutVertically { -12 }
            ) {
                if (aiDetection != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = CpdSpace.space2x)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(CpdRadius.radiusLg),
                            color = CpdColor.bgSubtleSecondary,
                            border = androidx.compose.foundation.BorderStroke(CpdBorderWidth.default, CpdColor.CoralRest.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(CpdSpace.space4x),
                                verticalArrangement = Arrangement.spacedBy(CpdSpace.space2x)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(CpdSpace.space15x)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(CpdRadius.radiusFull),
                                            color = CpdColor.CoralRest.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                aiDetection.platform.name.replace("_", " "),
                                                modifier = Modifier.padding(horizontal = CpdSpace.space2x, vertical = CpdSpace.space05x),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CpdColor.CoralRest
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(CpdRadius.radiusFull),
                                            color = CpdColor.borderInteractiveSecondary.copy(alpha = 0.4f)
                                        ) {
                                            Text(
                                                aiDetection.category.name,
                                                modifier = Modifier.padding(horizontal = CpdSpace.space2x, vertical = CpdSpace.space05x),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CpdColor.textSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        aiDetection.summaryBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CpdColor.CoralRest,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    aiDetection.cleanTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CpdColor.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = RoundedCornerShape(CpdRadius.radiusSm), color = CpdColor.bgCanvasDefault) {
                                        Text(
                                            "Format: .${aiDetection.suggestedFormat}",
                                            modifier = Modifier.padding(horizontal = CpdSpace.space15x, vertical = CpdSpace.space05x),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CpdColor.textSecondary
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(CpdRadius.radiusSm), color = CpdColor.bgCanvasDefault) {
                                        Text(
                                            "Quality: ${aiDetection.suggestedQuality}",
                                            modifier = Modifier.padding(horizontal = CpdSpace.space15x, vertical = CpdSpace.space05x),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CpdColor.textSecondary
                                        )
                                    }
                                    if (aiDetection.isDirectDownload) {
                                        Surface(shape = RoundedCornerShape(CpdRadius.radiusSm), color = CpdColor.CoralRest.copy(alpha = 0.2f)) {
                                            Text(
                                                "⚡ Direct Link Ready",
                                                modifier = Modifier.padding(horizontal = CpdSpace.space15x, vertical = CpdSpace.space05x),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CpdColor.CoralRest
                                            )
                                        }
                                    }
                                }

                                if (duplicateWarning?.isDuplicate == true) {
                                    Surface(
                                        shape = RoundedCornerShape(CpdRadius.radiusMd),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(CpdBorderWidth.default, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(CpdSpace.space2x),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)
                                        ) {
                                            Icon(HugeIcons.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(CpdSize.iconSm))
                                            Text(
                                                duplicateWarning?.message.orEmpty(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                BrandButton(
                                    onClick = { onQuickAiDownload(input) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(CpdSize.buttonLarge)
                                ) {
                                    Icon(HugeIcons.Download, null, Modifier.size(CpdSize.iconMd))
                                    Spacer(Modifier.width(CpdSpace.space2x))
                                    Text(
                                        "Choose quality & download",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Primary Download CTA when input is typed and AI detection not showing
            if (input.isNotBlank() && aiDetection == null) {
                BrandButton(
                    onClick = onSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CpdSpace.space3x)
                        .height(CpdSpace.space13x)
                ) {
                    Icon(HugeIcons.Download, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(CpdSpace.space2x))
                    Text("Download media or search", fontWeight = FontWeight.Normal)
                }
            }

            Spacer(Modifier.height(CpdSpace.space4x))

            // Platform Directory with Category Chips
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Favorite platforms", style = MaterialTheme.typography.titleSmall, color = CpdColor.textPrimary)
                TextButton(onClick = { onBrowse("") }) {
                    Text("Explore all", color = CpdColor.CoralRest)
                }
            }

            // Category Filter Chips (All, Video, Social, Audio, Community)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All", "Video", "Social", "Music", "Community").forEach { cat ->
                    FilterChip(
                        selected = platformCategory == cat,
                        onClick = { platformCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            val filteredPlatforms = when (platformCategory) {
                "Video" -> PlatformRegistry.platforms.filter { it.id in listOf("youtube", "vimeo", "dailymotion", "tiktok") }
                "Social" -> PlatformRegistry.platforms.filter { it.id in listOf("instagram", "tiktok", "facebook", "twitter") }
                "Music" -> PlatformRegistry.platforms.filter { it.id in listOf("soundcloud", "mixcloud", "youtube") }
                "Community" -> PlatformRegistry.platforms.filter { it.id in listOf("reddit", "pinterest", "bluesky", "threads") }
                else -> PlatformRegistry.platforms.take(8)
            }

            filteredPlatforms.take(8).chunked(4).forEach { row ->
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

            Spacer(Modifier.height(CpdSpace.space4x))

            // Secondary Shortcut Actions
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

                // Small breathing room above the app navbar; the column itself
                // remains vertically centered when the content fits the viewport.
                Spacer(Modifier.height(32.dp))
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
