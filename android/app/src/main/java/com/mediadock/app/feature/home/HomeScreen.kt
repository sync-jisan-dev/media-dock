package com.mediadock.app.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mediadock.app.R
import com.mediadock.app.core.designsystem.theme.bouncyClickable
import com.mediadock.app.domain.model.PlatformRegistry

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToInspect: (String) -> Unit,
    onBrowse: (String) -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val input by viewModel.urlInput.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val recent by viewModel.recentDownloads.collectAsState()
    var category by rememberSaveable { mutableStateOf("All") }
    val illustrationMotion = rememberInfiniteTransition(label = "illustrationFloat")
    val illustrationOffset by illustrationMotion.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "illustrationOffset"
    )
    fun openInput() {
        val value = input.trim()
        if (value.startsWith("https://") || value.startsWith("http://")) {
            if (viewModel.validateUrl()) onNavigateToInspect(value)
        } else if (value.isNotBlank()) onBrowse(value)
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            OutlinedTextField(
                value = input, onValueChange = viewModel::onUrlChange,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search or paste a video link", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = { IconButton(onClick = viewModel::pasteFromClipboard) { Icon(Icons.Rounded.ContentPaste, "Paste link", tint = MaterialTheme.colorScheme.primary) } },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { openInput() })
            )
            if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (input.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = { openInput() }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Rounded.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (input.trim().startsWith("http")) "Find download options" else "Search the web")
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().heightIn(min = 170.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(start = 20.dp, top = 20.dp, bottom = 20.dp)) {
                        Text("Watch it.\nSave it. Love it.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(8.dp))
                        Text("Your favorite media,\nwherever you go.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = { onBrowse("") },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Explore now", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Image(painterResource(R.drawable.media_dock_hero), null, Modifier.size(150.dp).graphicsLayer { translationY = illustrationOffset }, contentScale = ContentScale.Fit)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Your favorite platforms", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onBrowse("") }) { Text("See all") }
            }
            PlatformRegistry.platforms.take(8).chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { platform ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onBrowse("https://${platform.domains.first()}") }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            PlatformIcon(platform.id)
                            Spacer(Modifier.height(8.dp))
                            Text(platform.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Video", Icons.Rounded.SmartDisplay, Modifier.weight(1f)) { category = "Video" }
                QuickAction("Music", Icons.Rounded.Headphones, Modifier.weight(1f)) { category = "Music" }
                QuickAction("Images", Icons.Rounded.Collections, Modifier.weight(1f)) { category = "Images" }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your collection", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onLibrary) { Text("View all") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Video", "Music", "Images").forEach { title ->
                    FilterChip(selected = category == title, onClick = { category = title }, label = { Text(title) })
                }
            }
        }
        val filtered = recent.filter { category == "All" || it.mediaType == when (category) { "Video" -> "video"; "Music" -> "audio"; else -> "image" } }
        if (filtered.isEmpty()) {
            item {
                AnimatedContent(category, label = "collectionCategory") { selected ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.VideoLibrary, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text(if (selected == "All") "Make room for your favorites" else "Your ${selected.lowercase()} collection starts here", style = MaterialTheme.typography.titleSmall)
                            Text("Paste a link above to save your first download.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        items(filtered, key = { it.id }) { media ->
            Card(onClick = { onNavigateToInspect(media.sourceUrl) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(if (media.mediaType == "audio") Icons.Rounded.Headphones else Icons.Rounded.PlayCircle, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    AsyncImage(media.localUri, media.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(media.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                        Text(media.format.uppercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledIconButton(onClick = { onNavigateToInspect(media.sourceUrl) }) { Icon(Icons.Rounded.Download, "Download again") }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Share, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Found something you love?", style = MaterialTheme.typography.titleSmall)
                        Text("Tap Share in your favorite app and choose MediaDock.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformIcon(id: String, modifier: Modifier = Modifier) {
    val (icon, color) = when (id) {
        "youtube" -> Icons.Rounded.PlayArrow to Color(0xFFFF344D)
        "instagram" -> Icons.Rounded.CameraAlt to Color(0xFFC54FC7)
        "tiktok" -> Icons.Rounded.MusicNote to Color(0xFF25F4EE)
        "facebook" -> Icons.Rounded.Facebook to Color(0xFF1877F2)
        "pinterest" -> Icons.Rounded.PushPin to Color(0xFFE64B60)
        "soundcloud" -> Icons.Rounded.Cloud to Color(0xFFFF873F)
        "reddit" -> Icons.Rounded.Forum to Color(0xFFFF643D)
        else -> Icons.Rounded.Language to Color(0xFF249DE9)
    }
    Box(modifier.size(52.dp).background(color.copy(alpha = .10f), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(28.dp), tint = color)
    }
}

@Composable
private fun QuickAction(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.bouncyClickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}
