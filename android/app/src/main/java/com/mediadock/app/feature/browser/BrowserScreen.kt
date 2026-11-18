package com.mediadock.app.feature.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.luminance
import com.mediadock.app.R
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mediadock.app.core.common.MediaActions
import com.mediadock.app.core.datastore.BrowserTab
import com.mediadock.app.core.datastore.WorkspaceRules
import com.mediadock.app.core.designsystem.components.*
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.*
import com.mediadock.app.domain.model.PlatformRegistry
import com.mediadock.app.feature.home.PlatformIcon
import com.mediadock.app.feature.workspace.WorkspaceViewModel

@Composable fun BrowserScreen(
    initialUrl: String, onDownload: (String) -> Unit, modifier: Modifier = Modifier,
    onSavedLinks: () -> Unit = {}, workspace: WorkspaceViewModel = hiltViewModel(),
    browser: BrowserSessionViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    startOnEntry: Boolean = initialUrl.isBlank()
) {
    val context = LocalContext.current
    val session by browser.session.collectAsState()
    var showStart by remember { mutableStateOf(startOnEntry) }
    var initialHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialUrl) {
        browser.initialize("")
        if (!initialHandled && initialUrl.isNotBlank()) {
            if (!browser.newTab(BrowserRules.destination(initialUrl))) MediaActions.message(context, "Close a tab before opening another page.")
            initialHandled = true
        }
    }
    val tab = session.active
    if (!session.ready || tab == null) { MediaDockLoadingIndicator(message = "Restoring your browser"); return }
    key(tab.id) { BrowserPage(tab, browser, workspace, onDownload, onSavedLinks, modifier, showStart) { showStart = it } }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable private fun BrowserPage(tab: BrowserTab, browser: BrowserSessionViewModel, workspace: WorkspaceViewModel, onDownload: (String) -> Unit, onSavedLinks: () -> Unit, modifier: Modifier, showStart: Boolean, onStartChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val session by browser.session.collectAsState()
    val saved by workspace.state.collectAsState()
    var startQuery by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable(tab.id) { mutableStateOf(tab.url) }
    var currentUrl by remember { mutableStateOf(tab.url) }
    var pageTitle by remember { mutableStateOf(tab.title) }
    var progress by remember { mutableStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var tabsDialog by remember { mutableStateOf(false) }
    var find by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var matchCount by remember { mutableStateOf("0 matches") }
        var siteInfo by remember { mutableStateOf(false) }
    var zoomDialog by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(tab.textZoom.toFloat()) }
    var uploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var allowMultiple by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uploadCallback?.onReceiveValue(uris.takeIf { it.isNotEmpty() }?.let { if (allowMultiple) it.toTypedArray() else arrayOf(it.first()) }); uploadCallback = null
    }
    val downloadCallback by rememberUpdatedState(onDownload)
    var restoreScroll by remember { mutableStateOf(true) }
    val cookies = remember { CookieManager.getInstance().apply { setAcceptCookie(true) } }
    val webView = remember(tab.id) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.safeBrowsingEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.textZoom = tab.textZoom
            applyDesktop(tab.desktop, context)
            cookies.setAcceptThirdPartyCookies(this, true)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, value: Int) { progress = value }
                override fun onReceivedTitle(view: WebView, title: String?) {
                    pageTitle = title.orEmpty()
                    browser.updateTab(tab.id) { it.copy(title = title?.take(200).orEmpty().ifBlank { "New tab" }) }
                }
                override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                    uploadCallback?.onReceiveValue(null); uploadCallback = callback
                    allowMultiple = params.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                    filePicker.launch(params.acceptTypes.filter { it.isNotBlank() }.toTypedArray().ifEmpty { arrayOf("*/*") })
                    return true
                }
                override fun onShowCustomView(view: View, callback: CustomViewCallback) { fullscreen = view; fullscreenCallback = callback }
                override fun onHideCustomView() { fullscreen = null; fullscreenCallback?.onCustomViewHidden(); fullscreenCallback = null }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = request.url.scheme !in listOf("http", "https")
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    currentUrl = url.takeIf(WorkspaceRules::validUrl).orEmpty(); address = currentUrl; pageError = null
                    if (WorkspaceRules.validUrl(url)) browser.updateTab(tab.id) { it.copy(url = url) }
                }
                override fun onPageFinished(view: WebView, url: String) {
                    currentUrl = url.takeIf(WorkspaceRules::validUrl).orEmpty(); canGoBack = view.canGoBack(); canGoForward = view.canGoForward()
                    if (pageError == null) workspace.visit(url, view.title.orEmpty())
                    browser.updateTab(tab.id) { it.copy(url = currentUrl, title = if (currentUrl.isBlank()) "New tab" else view.title.orEmpty().take(200)) }
                    if (restoreScroll) { restoreScroll = false; view.post { view.scrollTo(0, tab.scrollY) } }
                    cookies.flush()
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) pageError = "This page could not be loaded. Check your connection and try again."
                }
            }
            setFindListener { active, total, done -> if (done) matchCount = if (total == 0) "No matches" else "${active + 1} of $total" }
            setDownloadListener { url, _, _, _, _ -> downloadCallback(url) }
        }
    }
    fun rememberPage() { val bundle = Bundle(); webView.saveState(bundle); browser.saveWebState(tab.id, bundle, webView.scrollY); cookies.flush() }
    fun closeFullscreen() { fullscreen = null; fullscreenCallback?.onCustomViewHidden(); fullscreenCallback = null }
    fun open(value: String) {
        val target = BrowserRules.destination(value)
        if (target.isBlank()) return
        focusManager.clearFocus(); keyboard?.hide()
        restoreScroll = false; currentUrl = target; address = target; pageError = null
        onStartChanged(false); browser.navigate(target); webView.onResume(); webView.loadUrl(target)
    }
    LaunchedEffect(webView) {
        val restored = browser.restoreWebState(tab.id)?.let { webView.restoreState(it) != null } ?: false
        if (!restored && tab.url.isNotBlank()) webView.loadUrl(tab.url)
    }
    DisposableEffect(webView, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_RESUME -> webView.onResume()
            Lifecycle.Event.ON_PAUSE -> { rememberPage(); webView.onPause() }
            else -> Unit
        } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); rememberPage(); uploadCallback?.onReceiveValue(null); uploadCallback = null; closeFullscreen(); webView.stopLoading(); webView.destroy() }
    }
    BackHandler(fullscreen != null || (!showStart && currentUrl.isNotBlank())) {
        if (fullscreen != null) closeFullscreen()
        else if (webView.canGoBack()) webView.goBack()
        else { onStartChanged(true); webView.onPause() }
    }
    if (tabsDialog) AlertDialog(onDismissRequest = { tabsDialog = false }, title = { Text("Your tabs · ${session.tabs.size}") }, text = {
        LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(session.tabs, key = { it.id }) { item ->
            Surface(shape = RoundedCornerShape(14.dp), color = if (item.id == session.activeId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onStartChanged(false); browser.selectTab(item.id); tabsDialog = false }.padding(14.dp)) {
                        Text(item.title.ifBlank { "New tab" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                        Text(BrowserRules.displayHost(item.url).ifBlank { "Start page" }, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { browser.closeTab(item.id) }) { Icon(HugeIcons.Close, "Close tab ${item.title}") }
                }
            }
        } }
    }, confirmButton = { TextButton(onClick = { if (browser.newTab()) { onStartChanged(true); tabsDialog = false } else MediaActions.message(context, "You can keep up to 12 tabs. Close a tab first.") }) { Text("New tab") } }, dismissButton = { TextButton(onClick = { tabsDialog = false }) { Text("Done") } })
    if (find) AlertDialog(onDismissRequest = { find = false; webView.clearMatches() }, title = { Text("Find in page") }, text = {
        Column { OutlinedTextField(findQuery, { findQuery = it; webView.findAllAsync(it) }, singleLine = true, label = { Text("Search page text") }); Text(matchCount, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall); Row { TextButton(onClick = { webView.findNext(false) }) { Text("Previous") }; TextButton(onClick = { webView.findNext(true) }) { Text("Next") } } }
    }, confirmButton = { TextButton(onClick = { find = false; webView.clearMatches() }) { Text("Done") } })
    if (siteInfo) AlertDialog(onDismissRequest = { siteInfo = false }, title = { Text(BrowserRules.displayHost(currentUrl).ifBlank { "MediaDock browser" }) }, text = { Text(if (currentUrl.startsWith("https://")) "This page uses HTTPS. Cookies and website storage are kept in MediaDock on this device. A website may still ask you to sign in again when its session expires." else "This page does not use HTTPS. Cookies and website storage are kept in MediaDock on this device.") }, confirmButton = { TextButton(onClick = { siteInfo = false }) { Text("Done") } })
    if (zoomDialog) AlertDialog(onDismissRequest = { webView.settings.textZoom = tab.textZoom; zoomDialog = false }, title = { Text("Page text size · ${zoom.toInt()}%") }, text = { Slider(zoom, { zoom = it; webView.settings.textZoom = it.toInt() }, valueRange = 75f..200f, steps = 4) }, confirmButton = { TextButton(onClick = { browser.updateTab(tab.id) { it.copy(textZoom = zoom.toInt()) }; zoomDialog = false }) { Text("Save") } })
    if (fullscreen != null) { Box(modifier.fillMaxSize()) { AndroidView(factory = { fullscreen!! }, Modifier.fillMaxSize()); FilledIconButton(onClick = { closeFullscreen() }, Modifier.align(Alignment.TopEnd).padding(12.dp)) { Icon(HugeIcons.Close, "Exit full screen") } }; return }
    Column(modifier.fillMaxSize().background(browserPageColor())) {
        if (!showStart && currentUrl.isNotBlank()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(address, { address = it }, Modifier.weight(1f), placeholder = { Text("Search or enter address", style = MaterialTheme.typography.bodySmall) }, singleLine = true,
                leadingIcon = { IconButton(onClick = { siteInfo = true }, enabled = currentUrl.isNotBlank()) { Icon(HugeIcons.Language, "Site information", Modifier.size(20.dp)) } },
                trailingIcon = { IconButton(onClick = { open(address) }) { Icon(HugeIcons.ArrowForward, "Open address", Modifier.size(20.dp)) } }, shape = RoundedCornerShape(24.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go), keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { open(address) }))
            IconButton(onClick = { tabsDialog = true }) { Surface(shape = RoundedCornerShape(7.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface)) { Text(session.tabs.size.toString(), Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium) } }
            Box { IconButton(onClick = { menu = true }) { Icon(HugeIcons.MoreVert, "Browser tools") }; DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("New tab") }, onClick = { if (!browser.newTab()) MediaActions.message(context, "Close a tab before opening another.") else onStartChanged(true); menu = false })
                DropdownMenuItem(text = { Text("Saved links & history") }, onClick = { onSavedLinks(); menu = false })
                if (currentUrl.isNotBlank()) {
                    DropdownMenuItem(text = { Text(if (saved.links.any { it.url == currentUrl }) "Bookmarked" else "Bookmark page") }, onClick = { workspace.save(currentUrl, pageTitle); menu = false; MediaActions.message(context, "Page saved") })
                    DropdownMenuItem(text = { Text("Find in page") }, onClick = { find = true; menu = false })
                    DropdownMenuItem(text = { Text(if (tab.desktop) "Use mobile site" else "Use desktop site") }, onClick = { webView.applyDesktop(!tab.desktop, context); browser.updateTab(tab.id) { it.copy(desktop = !it.desktop) }; webView.reload(); menu = false })
                    DropdownMenuItem(text = { Text("Text size") }, onClick = { zoom = tab.textZoom.toFloat(); zoomDialog = true; menu = false })
                    DropdownMenuItem(text = { Text("Copy page link") }, onClick = { MediaActions.copy(context, currentUrl); menu = false })
                    DropdownMenuItem(text = { Text("Share page") }, onClick = { MediaActions.shareText(context, currentUrl); menu = false })
                    DropdownMenuItem(text = { Text("Open in external browser") }, onClick = { MediaActions.openWeb(context, currentUrl); menu = false })
                }
            } }
        }
        }
        if (!showStart && currentUrl.isNotBlank()) {
            if (progress < 100) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth(), color = BrandOrange)
            if (pageError != null) Surface(color = MaterialTheme.colorScheme.secondaryContainer) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(pageError!!, style = MaterialTheme.typography.bodySmall); TextButton(onClick = { webView.reload() }) { Text("Retry page") } } }
            AndroidView(factory = { webView }, modifier = Modifier.weight(1f).fillMaxWidth())
        } else {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onSavedLinks) {
                                Icon(HugeIcons.FolderOpen, null, Modifier.size(18.dp), tint = CpdColor.CoralRest)
                                Spacer(Modifier.width(6.dp))
                                Text("Saved pages", color = CpdColor.textPrimary, style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(onClick = { tabsDialog = true }) {
                                Icon(HugeIcons.Collections, null, Modifier.size(18.dp), tint = CpdColor.CoralRest)
                                Spacer(Modifier.width(6.dp))
                                Text("${session.tabs.size} tabs", color = CpdColor.textPrimary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("Your space to explore.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CpdColor.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Search anything or visit a website.", style = MaterialTheme.typography.bodyMedium, color = CpdColor.textSecondary)
                        Spacer(Modifier.height(24.dp))
                    }
                    item { OutlinedTextField(startQuery, { startQuery = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search or enter web address") }, leadingIcon = { Icon(HugeIcons.Search, null) }, trailingIcon = { IconButton(onClick = { open(startQuery) }, enabled = startQuery.isNotBlank()) { Icon(HugeIcons.ArrowForward, "Search the web") } }, shape = RoundedCornerShape(28.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go), keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { open(startQuery) })); Spacer(Modifier.height(24.dp)) }
                    items(PlatformRegistry.platforms.take(8).chunked(4)) { platforms -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { platforms.forEach { platform -> Column(Modifier.weight(1f).clickable { open("https://${platform.domains.first()}") }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { PlatformIcon(platform.id); Spacer(Modifier.height(8.dp)); Text(platform.name, style = MaterialTheme.typography.labelSmall, color = CpdColor.textSecondary, maxLines = 1) } } } }
                    if (currentUrl.isNotBlank()) item { TextButton(onClick = { onStartChanged(false); webView.onResume() }) { Text("Continue browsing", color = CpdColor.CoralRest) } }
                }
            }
        }
        if (!showStart && currentUrl.isNotBlank()) {
        Surface(color = CpdColor.bgSubtleTertiary, contentColor = CpdColor.textPrimary) { Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { webView.goBack() }, enabled = canGoBack) { Icon(HugeIcons.ArrowBack, "Previous page", tint = if (canGoBack) CpdColor.textPrimary else CpdColor.textDisabled) }
            IconButton(onClick = { webView.goForward() }, enabled = canGoForward) { Icon(HugeIcons.ArrowForward, "Next page", tint = if (canGoForward) CpdColor.textPrimary else CpdColor.textDisabled) }
            IconButton(onClick = { onStartChanged(true); startQuery = ""; webView.onPause() }) { Icon(HugeIcons.Home, "Browser start page", tint = CpdColor.textPrimary) }
            IconButton(onClick = { if (progress < 100) { webView.stopLoading(); progress = 100 } else if (currentUrl.isNotBlank()) webView.reload() }, enabled = currentUrl.isNotBlank()) { Icon(if (progress < 100) HugeIcons.Close else HugeIcons.Refresh, if (progress < 100) "Stop loading" else "Reload page", tint = CpdColor.textPrimary) }
            BrandButton(onClick = { downloadCallback(currentUrl) }, enabled = WorkspaceRules.validUrl(currentUrl), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) { Icon(HugeIcons.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save") }
        } }
        }
    }
}

@Composable
fun browserPageColor() = com.mediadock.app.core.designsystem.theme.CpdColor.bgCanvasDefault

private fun WebView.applyDesktop(desktop: Boolean, context: Context) {
    val mobile = WebSettings.getDefaultUserAgent(context)
    settings.userAgentString = if (desktop) mobile.replace("; wv", "").replace(Regex("\\(Linux; Android[^)]*\\)"), "(X11; Linux x86_64)").replace(" Mobile ", " ") else mobile
    settings.useWideViewPort = desktop
    settings.loadWithOverviewMode = desktop
}
