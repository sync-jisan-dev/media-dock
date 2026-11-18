package com.mediadock.app.feature.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediadock.app.core.common.MediaActions
import com.mediadock.app.core.datastore.WorkspaceRules
import com.mediadock.app.core.designsystem.components.SectionHeading
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.domain.model.PlatformRegistry
import com.mediadock.app.feature.home.PlatformIcon
import com.mediadock.app.feature.workspace.WorkspaceViewModel
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@Composable fun BrowserScreen(initialUrl: String, onDownload: (String) -> Unit, modifier: Modifier = Modifier, onSavedLinks: () -> Unit = {}, workspace: WorkspaceViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val saved by workspace.state.collectAsState()
    var address by rememberSaveable { mutableStateOf(initialUrl) }
    var currentUrl by rememberSaveable { mutableStateOf("") }
    var pageTitle by rememberSaveable { mutableStateOf("") }
    var progress by remember { mutableStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var browsing by rememberSaveable { mutableStateOf(initialUrl.isNotBlank()) }
    var desktop by rememberSaveable { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var find by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var matchCount by remember { mutableStateOf("0 matches") }
    var platformQuery by rememberSaveable { mutableStateOf("") }
    val downloadCallback by rememberUpdatedState(onDownload)
    val visitCallback by rememberUpdatedState<(String, String) -> Unit>({ url, title -> workspace.visit(url, title) })
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, value: Int) { progress = value }
                override fun onReceivedTitle(view: WebView, title: String?) { pageTitle = title.orEmpty() }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = request.url.scheme !in listOf("http", "https")
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) { currentUrl = url; address = url; pageError = null }
                override fun onPageFinished(view: WebView, url: String) {
                    currentUrl = url; canGoBack = view.canGoBack(); canGoForward = view.canGoForward()
                    if (pageError == null) visitCallback(url, view.title.orEmpty())
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) pageError = "This page could not be loaded. Check your connection and try again."
                }
            }
            setFindListener { active, total, done -> if (done) matchCount = if (total == 0) "No matches" else "${active + 1} of $total" }
            setDownloadListener { url, _, _, _, _ -> downloadCallback(url) }
        }
    }
    fun open(value: String) {
        if (value.isBlank()) return
        val trimmed = value.trim()
        val target = when {
            WorkspaceRules.validUrl(trimmed) -> trimmed
            !trimmed.contains(' ') && trimmed.contains('.') && WorkspaceRules.validUrl("https://$trimmed") -> "https://$trimmed"
            else -> "https://duckduckgo.com/?q=${URLEncoder.encode(trimmed, "UTF-8")}"
        }
        browsing = true; currentUrl = target; webView.loadUrl(target)
    }
    LaunchedEffect(Unit) { if (browsing) open(currentUrl.ifBlank { initialUrl }) }
    DisposableEffect(webView) { onDispose { webView.stopLoading(); webView.destroy() } }
    BackHandler(browsing) { if (webView.canGoBack()) webView.goBack() else { webView.stopLoading(); browsing = false; currentUrl = ""; address = ""; pageError = null } }
    if (find) AlertDialog(onDismissRequest = { find = false; webView.clearMatches() }, title = { Text("Find in page") }, text = {
        Column {
            OutlinedTextField(findQuery, { findQuery = it; webView.findAllAsync(it) }, singleLine = true, label = { Text("Search page text") })
            Text(matchCount, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
            Row { TextButton(onClick = { webView.findNext(false) }) { Text("Previous") }; TextButton(onClick = { webView.findNext(true) }) { Text("Next") } }
        }
    }, confirmButton = { TextButton(onClick = { find = false; webView.clearMatches() }) { Text("Done") } })
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(address, { address = it }, Modifier.weight(1f), placeholder = { Text("Search or enter address") }, singleLine = true,
                leadingIcon = { Icon(HugeIcons.Search, null) }, trailingIcon = { IconButton(onClick = { open(address) }) { Icon(HugeIcons.ArrowForward, "Open address") } }, shape = RoundedCornerShape(18.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go), keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { open(address) }))
            if (browsing) Box {
                IconButton(onClick = { menu = true }) { Icon(HugeIcons.MoreVert, "Browser tools") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(if (saved.links.any { it.url == currentUrl }) "Bookmarked" else "Bookmark page") }, onClick = { workspace.save(currentUrl, pageTitle); menu = false; MediaActions.message(context, "Page saved to your links") })
                    DropdownMenuItem(text = { Text("Find in page") }, onClick = { find = true; menu = false })
                    DropdownMenuItem(text = { Text(if (desktop) "Use mobile site" else "Use desktop site") }, onClick = {
                        desktop = !desktop
                        webView.settings.userAgentString = if (desktop) "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" else WebSettings.getDefaultUserAgent(context)
                        webView.settings.useWideViewPort = desktop; webView.settings.loadWithOverviewMode = desktop; webView.reload(); menu = false
                    })
                    DropdownMenuItem(text = { Text("Copy page link") }, onClick = { MediaActions.copy(context, currentUrl); menu = false })
                    DropdownMenuItem(text = { Text("Share page") }, onClick = { MediaActions.shareText(context, currentUrl); menu = false })
                    DropdownMenuItem(text = { Text("Open in external browser") }, onClick = { MediaActions.openWeb(context, currentUrl); menu = false })
                    DropdownMenuItem(text = { Text("Close page") }, onClick = { webView.stopLoading(); browsing = false; address = ""; pageError = null; menu = false })
                }
            }
        }
        if (browsing) {
            if (progress < 100) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            if (pageError != null) Surface(color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(pageError!!, style = MaterialTheme.typography.bodySmall); TextButton(onClick = { webView.reload() }) { Text("Retry page") } } }
            AndroidView(factory = { webView }, modifier = Modifier.weight(1f).fillMaxWidth())
            Surface {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { webView.goBack() }, enabled = canGoBack) { Icon(HugeIcons.ArrowBack, "Previous page") }
                    IconButton(onClick = { webView.goForward() }, enabled = canGoForward) { Icon(HugeIcons.ArrowForward, "Next page") }
                    IconButton(onClick = { if (progress < 100) { webView.stopLoading(); progress = 100 } else webView.reload() }) { Icon(if (progress < 100) HugeIcons.Close else HugeIcons.Refresh, if (progress < 100) "Stop loading" else "Reload page") }
                    Button(onClick = { downloadCallback(currentUrl) }, enabled = WorkspaceRules.validUrl(currentUrl)) { Icon(HugeIcons.Download, null); Spacer(Modifier.width(6.dp)); Text("Download") }
                }
            }
        } else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionHeading("Explore the web", "Your platforms. Your next discovery.") }
            item { Card(onClick = onSavedLinks, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(HugeIcons.FolderOpen, null, Modifier.size(32.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Your saved links", style = MaterialTheme.typography.titleMedium); Text("${saved.links.size} bookmarks · ${saved.recentPages.size} recent pages", style = MaterialTheme.typography.bodySmall) }; Icon(HugeIcons.ChevronRight, null) }
            } }
            if (saved.recentPages.isNotEmpty()) item { TextButton(onClick = { open(saved.recentPages.first().url) }) { Icon(HugeIcons.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Continue browsing", maxLines = 1) } }
            item { OutlinedTextField(platformQuery, { platformQuery = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Find a platform") }, leadingIcon = { Icon(HugeIcons.Search, null) }, shape = RoundedCornerShape(16.dp)) }
            val platforms = PlatformRegistry.platforms.filter { it.name.contains(platformQuery, true) || it.domains.any { domain -> domain.contains(platformQuery, true) } }
            if (platforms.isEmpty()) item { Text("No platforms match your search.", style = MaterialTheme.typography.bodyMedium) }
            items(platforms, key = { it.id }) { platform ->
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clickable { open("https://${platform.domains.first()}") }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { PlatformIcon(platform.id); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(platform.name, style = MaterialTheme.typography.titleMedium); Text(platform.supportedTypes.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(HugeIcons.ChevronRight, null) }
                }
            }
        }
    }
}
