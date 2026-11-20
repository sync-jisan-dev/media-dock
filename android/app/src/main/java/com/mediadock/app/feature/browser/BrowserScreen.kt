package com.mediadock.app.feature.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.mediadock.app.domain.model.SupportedPlatform
import com.mediadock.app.feature.home.PlatformIcon
import com.mediadock.app.feature.workspace.WorkspaceViewModel

@Composable
fun BrowserScreen(
    initialUrl: String,
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSavedLinks: () -> Unit = {},
    workspace: WorkspaceViewModel = hiltViewModel(),
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
            if (!browser.newTab(BrowserRules.destination(initialUrl))) {
                MediaActions.message(context, "Close a tab before opening another page.")
            }
            initialHandled = true
        }
    }

    val tab = session.active
    if (!session.ready || tab == null) {
        MediaDockLoadingIndicator(message = "Restoring your browser")
        return
    }

    key(tab.id) {
        BrowserPage(
            tab = tab,
            browser = browser,
            workspace = workspace,
            onDownload = onDownload,
            onSavedLinks = onSavedLinks,
            modifier = modifier,
            showStart = showStart
        ) { showStart = it }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserPage(
    tab: BrowserTab,
    browser: BrowserSessionViewModel,
    workspace: WorkspaceViewModel,
    onDownload: (String) -> Unit,
    onSavedLinks: () -> Unit,
    modifier: Modifier,
    showStart: Boolean,
    onStartChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val session by browser.session.collectAsState()
    val saved by workspace.state.collectAsState()

    var startQuery by rememberSaveable { mutableStateOf("") }
    var selectedEngine by rememberSaveable { mutableStateOf("Google") }
    var address by rememberSaveable(tab.id) { mutableStateOf(tab.url) }
    var currentUrl by remember { mutableStateOf(tab.url) }
    var pageTitle by remember { mutableStateOf(tab.title) }
    var progress by remember { mutableStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var tabsDialog by remember { mutableStateOf(false) }
    var findInPage by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var matchCount by remember { mutableStateOf("0 of 0") }
    var siteInfoDialog by remember { mutableStateOf(false) }
    var zoomDialog by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(tab.textZoom.toFloat()) }
    var editingAddress by remember { mutableStateOf(false) }
    var detectedMediaUrl by remember { mutableStateOf<String?>(null) }
    var adBlockEnabled by rememberSaveable { mutableStateOf(true) }
    var blockedTrackersCount by remember { mutableStateOf(0) }
    var isIncognito by rememberSaveable { mutableStateOf(false) }
    var readerMode by remember { mutableStateOf(false) }
    var forceDarkMode by remember { mutableStateOf(false) }
    var shieldDialog by remember { mutableStateOf(false) }
    var userAgentDialog by remember { mutableStateOf(false) }
    var uploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var allowMultiple by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uploadCallback?.onReceiveValue(
            uris.takeIf { it.isNotEmpty() }?.let { if (allowMultiple) it.toTypedArray() else arrayOf(it.first()) }
        )
        uploadCallback = null
    }

    val downloadCallback by rememberUpdatedState(onDownload)
    var restoreScroll by remember { mutableStateOf(true) }
    val isBookmarked = remember(currentUrl, saved.links) {
        saved.links.any { it.url == currentUrl }
    }

    val webView = remember(tab.id) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
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
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, value: Int) {
                    progress = value
                }
                override fun onReceivedTitle(view: WebView, title: String?) {
                    pageTitle = title.orEmpty()
                    browser.updateTab(tab.id) { it.copy(title = title?.take(200).orEmpty().ifBlank { "New tab" }) }
                }
                override fun onShowFileChooser(
                    view: WebView,
                    callback: ValueCallback<Array<Uri>>,
                    params: FileChooserParams
                ): Boolean {
                    uploadCallback?.onReceiveValue(null)
                    uploadCallback = callback
                    allowMultiple = params.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                    filePicker.launch(params.acceptTypes.filter { it.isNotBlank() }.toTypedArray().ifEmpty { arrayOf("*/*") })
                    return true
                }
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    fullscreen = view
                    fullscreenCallback = callback
                }
                override fun onHideCustomView() {
                    fullscreen = null
                    fullscreenCallback?.onCustomViewHidden()
                    fullscreenCallback = null
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return request.url.scheme !in listOf("http", "https")
                }
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    val reqUrl = request.url.toString().lowercase()
                    if (adBlockEnabled) {
                        val isAd = reqUrl.contains("googleads") || reqUrl.contains("doubleclick") ||
                                   reqUrl.contains("/ads.") || reqUrl.contains("adservice") ||
                                   reqUrl.contains("analytics") || reqUrl.contains("telemetry") ||
                                   reqUrl.contains("facebook.net/tr")
                        if (isAd) {
                            blockedTrackersCount++
                            return WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream("".toByteArray()))
                        }
                    }
                    if (reqUrl.endsWith(".mp4") || reqUrl.endsWith(".webm") || reqUrl.endsWith(".m4a") ||
                        reqUrl.endsWith(".mp3") || reqUrl.contains(".m3u8") || reqUrl.contains("videoplayback")) {
                        detectedMediaUrl = request.url.toString()
                    }
                    return super.shouldInterceptRequest(view, request)
                }
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    currentUrl = url.takeIf(WorkspaceRules::validUrl).orEmpty()
                    address = currentUrl
                    pageError = null
                    blockedTrackersCount = 0
                    detectedMediaUrl = null
                    if (WorkspaceRules.validUrl(url)) browser.updateTab(tab.id) { it.copy(url = url) }
                }
                override fun onPageFinished(view: WebView, url: String) {
                    currentUrl = url.takeIf(WorkspaceRules::validUrl).orEmpty()
                    canGoBack = view.canGoBack()
                    canGoForward = view.canGoForward()
                    if (pageError == null) workspace.visit(url, view.title.orEmpty())
                    browser.updateTab(tab.id) {
                        it.copy(url = currentUrl, title = if (currentUrl.isBlank()) "New tab" else view.title.orEmpty().take(200))
                    }
                    if (restoreScroll) {
                        restoreScroll = false
                        view.post { view.scrollTo(0, tab.scrollY) }
                    }
                    CookieManager.getInstance().flush()
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) {
                        pageError = "This page could not be loaded. Check your connection or search again."
                    }
                }
            }

            setFindListener { active, total, done ->
                if (done) matchCount = if (total == 0) "No matches" else "${active + 1} of $total"
            }
            setDownloadListener { url, _, _, _, _ -> downloadCallback(url) }
        }
    }

    fun rememberPage() {
        val bundle = Bundle()
        webView.saveState(bundle)
        browser.saveWebState(tab.id, bundle, webView.scrollY)
        CookieManager.getInstance().flush()
    }

    fun closeFullscreen() {
        fullscreen = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
    }

    fun open(value: String, engine: String = selectedEngine) {
        val target = BrowserRules.destination(value, engine)
        if (target.isBlank()) return
        focusManager.clearFocus()
        keyboard?.hide()
        editingAddress = false
        restoreScroll = false
        currentUrl = target
        address = target
        pageError = null
        onStartChanged(false)
        browser.navigate(target)
        webView.onResume()
        webView.loadUrl(target)
    }

    LaunchedEffect(webView) {
        val restored = browser.restoreWebState(tab.id)?.let { webView.restoreState(it) != null } ?: false
        if (!restored && tab.url.isNotBlank()) webView.loadUrl(tab.url)
    }

    LaunchedEffect(readerMode) {
        if (readerMode) {
            webView.evaluateJavascript(
                "document.querySelectorAll('header,nav,footer,aside,.ad,.advertisement,#comments').forEach(e => e.style.display='none'); document.body.style.fontSize='18px'; document.body.style.lineHeight='1.6';",
                null
            )
        } else if (currentUrl.isNotBlank()) {
            webView.reload()
        }
    }

    LaunchedEffect(forceDarkMode) {
        if (forceDarkMode) {
            webView.evaluateJavascript(
                "document.documentElement.style.filter='invert(90%) hue-rotate(180deg)'; document.querySelectorAll('img, video').forEach(e => e.style.filter='invert(100%) hue-rotate(180deg)');",
                null
            )
        } else {
            webView.evaluateJavascript("document.documentElement.style.filter='';", null)
        }
    }

    DisposableEffect(webView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    rememberPage()
                    webView.onPause()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            rememberPage()
            uploadCallback?.onReceiveValue(null)
            uploadCallback = null
            closeFullscreen()
            webView.stopLoading()
            webView.destroy()
        }
    }

    // Chrome-like Responsible Back Navigation
    BackHandler(
        enabled = fullscreen != null || findInPage || tabsDialog || editingAddress || menu || (!showStart && currentUrl.isNotBlank())
    ) {
        when {
            fullscreen != null -> closeFullscreen()
            findInPage -> {
                findInPage = false
                webView.clearMatches()
            }
            tabsDialog -> tabsDialog = false
            editingAddress -> {
                editingAddress = false
                address = currentUrl
            }
            menu -> menu = false
            webView.canGoBack() -> webView.goBack()
            else -> {
                onStartChanged(true)
                webView.onPause()
            }
        }
    }

    // Fullscreen video/media overlay
    if (fullscreen != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { fullscreen!! }, modifier = Modifier.fillMaxSize())
            FilledIconButton(
                onClick = { closeFullscreen() },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(HugeIcons.Close, "Exit full screen", tint = Color.White)
            }
        }
        return
    }

    // Chrome-Style Tab Switcher
    if (tabsDialog) {
        AlertDialog(
            onDismissRequest = { tabsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tabs (${session.tabs.size} of ${BrowserRules.MAX_TABS})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal
                    )
                    TextButton(onClick = {
                        browser.closeAllTabs()
                        onStartChanged(true)
                        tabsDialog = false
                    }) {
                        Text("Close all", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(CpdSpace.space2x)
                ) {
                    items(session.tabs, key = { it.id }) { item ->
                        val isActive = item.id == session.activeId
                        Surface(
                            shape = RoundedCornerShape(CpdRadius.radiusLg),
                            color = if (isActive) BrandCoralOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isActive) BorderStroke(CpdBorderWidth.focus, BrandCoralOrange) else BorderStroke(CpdBorderWidth.hairline * 2, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = CpdSpace.space3x, vertical = CpdSpace.space2x),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onStartChanged(item.url.isBlank())
                                            browser.selectTab(item.id)
                                            tabsDialog = false
                                        }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isActive) {
                                            Surface(
                                                shape = CircleShape,
                                                color = BrandCoralOrange,
                                                modifier = Modifier.size(7.dp)
                                            ) {}
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = item.title.ifBlank { "New tab" },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isActive) FontWeight.Normal else FontWeight.Light
                                        )
                                    }
                                    Text(
                                        text = BrowserRules.displayHost(item.url).ifBlank { "Start page" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        browser.closeTab(item.id)
                                        if (session.tabs.size <= 1) {
                                            onStartChanged(true)
                                        }
                                    }
                                ) {
                                    Icon(HugeIcons.Close, "Close tab", Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (browser.newTab()) {
                            onStartChanged(true)
                            tabsDialog = false
                        } else {
                            MediaActions.message(context, "You can keep up to 12 tabs. Close a tab first.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCoralOrange)
                ) {
                    Icon(HugeIcons.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New tab")
                }
            },
            dismissButton = {
                TextButton(onClick = { tabsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Site Security / SSL Dialog
    if (siteInfoDialog) {
        val isHttps = currentUrl.startsWith("https://")
        AlertDialog(
            onDismissRequest = { siteInfoDialog = false },
            icon = {
                Icon(
                    imageVector = if (isHttps) HugeIcons.CheckCircle else HugeIcons.ErrorOutline,
                    contentDescription = null,
                    tint = if (isHttps) Color(0xFF10B981) else BrandCoralOrange,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (isHttps) "Connection is secure" else "Connection not secure",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = BrowserRules.displayHost(currentUrl).ifBlank { "Current site" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isHttps)
                            "Your information (passwords, cookies, and tokens) is private and encrypted with HTTPS when sent to this site."
                        else
                            "This website does not use modern encryption. Avoid entering sensitive credentials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Cookies & website cache are stored securely in MediaDock on this device.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { siteInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Text Zoom Slider Dialog
    if (zoomDialog) {
        AlertDialog(
            onDismissRequest = {
                webView.settings.textZoom = tab.textZoom
                zoomDialog = false
            },
            title = { Text("Text scaling · ${zoom.toInt()}%") },
            text = {
                Column {
                    Slider(
                        value = zoom,
                        onValueChange = {
                            zoom = it
                            webView.settings.textZoom = it.toInt()
                        },
                        valueRange = 75f..200f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandCoralOrange,
                            activeTrackColor = BrandCoralOrange
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("75% (Small)", style = MaterialTheme.typography.labelSmall)
                        Text("100% (Normal)", style = MaterialTheme.typography.labelSmall)
                        Text("200% (Large)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    browser.updateTab(tab.id) { it.copy(textZoom = zoom.toInt()) }
                    zoomDialog = false
                }) {
                    Text("Apply")
                }
            }
        )
    }

    // MediaDock Shield (Ad & Tracker Blocker Dialog)
    if (shieldDialog) {
        AlertDialog(
            onDismissRequest = { shieldDialog = false },
            icon = {
                Icon(
                    imageVector = HugeIcons.Security,
                    contentDescription = null,
                    tint = if (adBlockEnabled) Color(0xFF10B981) else BrandCoralOrange,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("MediaDock Shield", textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ad & Tracker Blocker", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = adBlockEnabled,
                            onCheckedChange = { adBlockEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandCoralOrange, checkedTrackColor = BrandCoralOrange.copy(alpha = 0.5f))
                        )
                    }
                    Surface(shape = RoundedCornerShape(CpdRadius.radiusMd), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(CpdSpace.space3x), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Trackers blocked on this page", style = MaterialTheme.typography.bodySmall)
                            Text("$blockedTrackersCount", style = MaterialTheme.typography.titleSmall, color = BrandCoralOrange)
                        }
                    }
                    Text("Blocks invasive ad scripts, crypto miners, and telemetry trackers for faster, safer page loading.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { shieldDialog = false; webView.reload() }) {
                    Text("Apply & Reload")
                }
            },
            dismissButton = {
                TextButton(onClick = { shieldDialog = false }) { Text("Close") }
            }
        )
    }

    // User-Agent Switcher Dialog
    if (userAgentDialog) {
        val userAgents = listOf(
            "Android Mobile (Default)" to "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            "Desktop (Chrome / Linux)" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "iPhone (Safari / iOS 17)" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
            "iPad Pro (Desktop Safari)" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
        )
        AlertDialog(
            onDismissRequest = { userAgentDialog = false },
            title = { Text("Choose User-Agent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    userAgents.forEach { (name, ua) ->
                        Surface(
                            shape = RoundedCornerShape(CpdRadius.radiusMd),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().clickable {
                                webView.settings.userAgentString = ua
                                userAgentDialog = false
                                webView.reload()
                                Toast.makeText(context, "User-Agent switched to $name", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(name, modifier = Modifier.padding(CpdSpace.space3x), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { userAgentDialog = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = modifier.fillMaxSize().background(browserPageColor())) {
        // TOP APP BAR / OMNIBOX (When in browsing mode)
        if (!showStart && currentUrl.isNotBlank()) {
            Surface(
                color = CpdColor.bgCanvasDefault,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth()) {
                    // Inline Find in Page Bar
                    if (findInPage) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = findQuery,
                                onValueChange = {
                                    findQuery = it
                                    webView.findAllAsync(it)
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Find on page…", style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                trailingIcon = {
                                    Text(
                                        text = matchCount,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                },
                                shape = RoundedCornerShape(CpdRadius.radiusMd)
                            )
                            IconButton(onClick = { webView.findNext(false) }) {
                                Icon(HugeIcons.ChevronRight, "Previous", Modifier.size(20.dp))
                            }
                            IconButton(onClick = { webView.findNext(true) }) {
                                Icon(HugeIcons.ArrowForward, "Next", Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                findInPage = false
                                webView.clearMatches()
                            }) {
                                Icon(HugeIcons.Close, "Close search", Modifier.size(20.dp))
                            }
                        }
                    } else if (editingAddress) {
                        // Address Editing State
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Search or type web address") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(HugeIcons.Search, null, Modifier.size(CpdSize.iconMd))
                                    },
                                    trailingIcon = {
                                        if (address.isNotBlank()) {
                                            IconButton(onClick = { address = "" }) {
                                                Icon(HugeIcons.Close, "Clear", Modifier.size(CpdSize.iconMd))
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(CpdRadius.radiusMd),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Go
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onGo = { open(address) }
                                    )
                                )
                                Spacer(Modifier.width(CpdSpace.space2x))
                                IconButton(
                                    onClick = { open(address) },
                                    enabled = address.isNotBlank()
                                ) {
                                    Icon(HugeIcons.ArrowForward, "Go", tint = BrandCoralOrange)
                                }
                                IconButton(onClick = {
                                    editingAddress = false
                                    address = currentUrl
                                }) {
                                    Icon(HugeIcons.Close, "Cancel")
                                }
                            }
                            // Search engine quick switcher
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = CpdSpace.space1x),
                                horizontalArrangement = Arrangement.spacedBy(CpdSpace.space15x)
                            ) {
                                listOf("Google", "DuckDuckGo", "Bing").forEach { engine ->
                                    FilterChip(
                                        selected = selectedEngine == engine,
                                        onClick = { selectedEngine = engine },
                                        label = { Text(engine, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandCoralOrange.copy(alpha = 0.15f),
                                            selectedLabelColor = BrandCoralOrange
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Standard Chrome Omnibox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CpdSpace.space3x, vertical = CpdSpace.space15x),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Omnibox Capsule
                            Surface(
                                shape = RoundedCornerShape(CpdRadius.radiusMd),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        editingAddress = true
                                        address = currentUrl
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = CpdSpace.space3x, vertical = CpdSpace.space2x),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isIncognito) {
                                        Surface(
                                            shape = RoundedCornerShape(CpdRadius.radiusFull),
                                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                            modifier = Modifier.padding(end = CpdSpace.space15x)
                                        ) {
                                            Text(
                                                "Private",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF8B5CF6),
                                                modifier = Modifier.padding(horizontal = CpdSpace.space2x, vertical = CpdSpace.space05x)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { shieldDialog = true },
                                        modifier = Modifier.size(CpdSize.iconLg)
                                    ) {
                                        Icon(
                                            imageVector = HugeIcons.Security,
                                            contentDescription = "Shield",
                                            tint = if (adBlockEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (currentUrl.startsWith("https://")) HugeIcons.Language else HugeIcons.ErrorOutline,
                                        contentDescription = "SSL Status",
                                        tint = if (currentUrl.startsWith("https://")) Color(0xFF10B981) else BrandCoralOrange,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { siteInfoDialog = true }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = BrowserRules.displayHost(currentUrl).ifBlank { currentUrl },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Refresh / Stop Icon
                                    IconButton(
                                        onClick = {
                                            if (progress < 100) {
                                                webView.stopLoading()
                                                progress = 100
                                            } else {
                                                webView.reload()
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (progress < 100) HugeIcons.Close else HugeIcons.Refresh,
                                            contentDescription = if (progress < 100) "Stop" else "Reload",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // Chrome-style Tab Counter Badge [ N ]
                            Surface(
                                shape = RoundedCornerShape(CpdRadius.radiusMd),
                                border = BorderStroke(CpdBorderWidth.hairline * 2, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .clickable { tabsDialog = true }
                                    .padding(horizontal = CpdSpace.space05x)
                            ) {
                                Text(
                                    text = session.tabs.size.toString(),
                                    modifier = Modifier.padding(horizontal = CpdSpace.space15x, vertical = CpdSpace.space05x),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // 3-Dots Chrome Menu
                            Box {
                                IconButton(onClick = { menu = true }) {
                                    Icon(HugeIcons.MoreVert, "More actions")
                                }
                                DropdownMenu(
                                    expanded = menu,
                                    onDismissRequest = { menu = false }
                                ) {
                                    // Quick action toolbar at top of Chrome menu
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { webView.goBack(); menu = false }, enabled = canGoBack) {
                                            Icon(HugeIcons.ArrowBack, "Back", tint = if (canGoBack) CpdColor.textPrimary else CpdColor.textDisabled)
                                        }
                                        IconButton(onClick = { webView.goForward(); menu = false }, enabled = canGoForward) {
                                            Icon(HugeIcons.ArrowForward, "Forward", tint = if (canGoForward) CpdColor.textPrimary else CpdColor.textDisabled)
                                        }
                                        IconButton(onClick = {
                                            if (isBookmarked) {
                                                workspace.removeLink(currentUrl)
                                                MediaActions.message(context, "Removed from saved pages")
                                            } else {
                                                workspace.save(currentUrl, pageTitle)
                                                MediaActions.message(context, "Saved page")
                                            }
                                            menu = false
                                        }) {
                                            Icon(
                                                imageVector = HugeIcons.PushPin,
                                                contentDescription = "Bookmark",
                                                tint = if (isBookmarked) BrandCoralOrange else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(onClick = { downloadCallback(currentUrl); menu = false }) {
                                            Icon(HugeIcons.Download, "Download", tint = BrandCoralOrange)
                                        }
                                        IconButton(onClick = { webView.reload(); menu = false }) {
                                            Icon(HugeIcons.Refresh, "Reload")
                                        }
                                    }
                                    HorizontalDivider()

                                    DropdownMenuItem(
                                        text = { Text("New tab") },
                                        leadingIcon = { Icon(HugeIcons.PlayCircle, null) },
                                        onClick = {
                                            if (browser.newTab()) onStartChanged(true)
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Saved pages & history") },
                                        leadingIcon = { Icon(HugeIcons.FolderOpen, null) },
                                        onClick = {
                                            onSavedLinks()
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Find in page") },
                                        leadingIcon = { Icon(HugeIcons.Search, null) },
                                        onClick = {
                                            findInPage = true
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("MediaDock Shield ($blockedTrackersCount blocked)")
                                                Icon(HugeIcons.Security, null, tint = if (adBlockEnabled) Color(0xFF10B981) else Color.Gray, modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        onClick = {
                                            shieldDialog = true
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("Private browsing")
                                                Checkbox(checked = isIncognito, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF8B5CF6)))
                                            }
                                        },
                                        onClick = {
                                            isIncognito = !isIncognito
                                            Toast.makeText(context, if (isIncognito) "Private mode on" else "Normal mode", Toast.LENGTH_SHORT).show()
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("Reader mode")
                                                Checkbox(checked = readerMode, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = BrandCoralOrange))
                                            }
                                        },
                                        onClick = {
                                            readerMode = !readerMode
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("Force dark web")
                                                Checkbox(checked = forceDarkMode, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = BrandCoralOrange))
                                            }
                                        },
                                        onClick = {
                                            forceDarkMode = !forceDarkMode
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Switch User-Agent") },
                                        leadingIcon = { Icon(HugeIcons.Explore, null) },
                                        onClick = {
                                            userAgentDialog = true
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Desktop site")
                                                Checkbox(
                                                    checked = tab.desktop,
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(checkedColor = BrandCoralOrange)
                                                )
                                            }
                                        },
                                        onClick = {
                                            val newDesktop = !tab.desktop
                                            webView.applyDesktop(newDesktop, context)
                                            browser.updateTab(tab.id) { it.copy(desktop = newDesktop) }
                                            webView.reload()
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Text size (${tab.textZoom}%)") },
                                        leadingIcon = { Icon(HugeIcons.Language, null) },
                                        onClick = {
                                            zoom = tab.textZoom.toFloat()
                                            zoomDialog = true
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Copy page link") },
                                        leadingIcon = { Icon(HugeIcons.ContentPaste, null) },
                                        onClick = {
                                            MediaActions.copy(context, currentUrl)
                                            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share page") },
                                        leadingIcon = { Icon(HugeIcons.Share, null) },
                                        onClick = {
                                            MediaActions.shareText(context, currentUrl)
                                            menu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open in external browser") },
                                        leadingIcon = { Icon(HugeIcons.Explore, null) },
                                        onClick = {
                                            MediaActions.openWeb(context, currentUrl)
                                            menu = false
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Clear browsing data", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(HugeIcons.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            CookieManager.getInstance().removeAllCookies(null)
                                            webView.clearCache(true)
                                            webView.clearHistory()
                                            Toast.makeText(context, "Cookies and browser cache cleared", Toast.LENGTH_SHORT).show()
                                            menu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Chrome Loading Progress Bar
                    if (progress < 100) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(2.5.dp),
                            color = BrandCoralOrange,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }

        // CONTENT AREA (WebView OR Start Page)
        if (!showStart && currentUrl.isNotBlank()) {
            // Web Page View
            if (pageError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth().padding(CpdSpace.space3x),
                    shape = RoundedCornerShape(CpdRadius.radiusLg)
                ) {
                    Column(Modifier.padding(CpdSpace.space4x)) {
                        Text(pageError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { webView.reload() }) {
                                Text("Retry", color = BrandCoralOrange)
                            }
                            TextButton(onClick = { MediaActions.openWeb(context, currentUrl) }) {
                                Text("Open externally")
                            }
                        }
                    }
                }
            }

            AndroidView(
                factory = { webView },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            // Floating Media Sniffer Bar (Auto-detected video/audio stream)
            AnimatedVisibility(
                visible = detectedMediaUrl != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(CpdRadius.radiusLg),
                    color = BrandCoralOrange,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(horizontal = CpdSpace.space3x, vertical = CpdSpace.space15x).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = CpdSpace.space35x, vertical = CpdSpace.space2x),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(HugeIcons.PlayCircle, null, tint = Color.White, modifier = Modifier.size(CpdSize.iconLg))
                        Spacer(Modifier.width(CpdSpace.space2x))
                        Column(Modifier.weight(1f)) {
                            Text("Media Stream Detected", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(BrowserRules.displayHost(detectedMediaUrl ?: ""), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = {
                                detectedMediaUrl?.let { downloadCallback(it) }
                                detectedMediaUrl = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandCoralOrange),
                            shape = RoundedCornerShape(CpdRadius.radiusFull),
                            modifier = Modifier.height(CpdSize.buttonSmall),
                            contentPadding = PaddingValues(horizontal = CpdSpace.space3x)
                        ) {
                            Text("Download", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { detectedMediaUrl = null }) {
                            Icon(HugeIcons.Close, "Dismiss", tint = Color.White, modifier = Modifier.size(CpdSize.iconMd))
                        }
                    }
                }
            }

            // Chrome Bottom Navigation Toolbar
            Surface(
                color = CpdColor.bgSubtleTertiary,
                contentColor = CpdColor.textPrimary,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CpdSpace.space3x, vertical = CpdSpace.space15x),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { webView.goBack() },
                        enabled = canGoBack
                    ) {
                        Icon(
                            HugeIcons.ArrowBack,
                            "Back",
                            tint = if (canGoBack) CpdColor.textPrimary else CpdColor.textDisabled
                        )
                    }
                    IconButton(
                        onClick = { webView.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(
                            HugeIcons.ArrowForward,
                            "Forward",
                            tint = if (canGoForward) CpdColor.textPrimary else CpdColor.textDisabled
                        )
                    }
                    IconButton(
                        onClick = {
                            onStartChanged(true)
                            startQuery = ""
                            webView.onPause()
                        }
                    ) {
                        Icon(HugeIcons.Home, "Start page", tint = CpdColor.textPrimary)
                    }
                    IconButton(onClick = { tabsDialog = true }) {
                        Surface(
                            shape = RoundedCornerShape(CpdRadius.radiusSm),
                            border = BorderStroke(CpdBorderWidth.hairline * 2, CpdColor.textPrimary),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = session.tabs.size.toString(),
                                modifier = Modifier.padding(horizontal = CpdSpace.space15x, vertical = CpdSpace.space05x),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    BrandButton(
                        onClick = { downloadCallback(currentUrl) },
                        enabled = WorkspaceRules.validUrl(currentUrl),
                        contentPadding = PaddingValues(horizontal = CpdSpace.space35x, vertical = CpdSpace.space2x)
                    ) {
                        Icon(HugeIcons.Download, null, Modifier.size(CpdSize.iconSm))
                        Spacer(Modifier.width(CpdSpace.space15x))
                        Text("Save")
                    }
                }
            }
        } else {
            // Chrome Start Page / New Tab
            Column(Modifier.weight(1f).fillMaxWidth()) {
                // Pinned Top Header Bar in the top corners of the screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-Left: Saved pages
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(CpdRadius.radiusFull))
                            .clickable(onClick = onSavedLinks)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = HugeIcons.FolderOpen,
                            contentDescription = "Saved pages",
                            modifier = Modifier.size(20.dp),
                            tint = CpdColor.CoralRest
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Saved pages",
                            color = CpdColor.textPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Top-Right: Browser Tab Switcher Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(CpdRadius.radiusFull))
                            .clickable { tabsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        // Chrome-style Tab Box [ N ]
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = CpdColor.CoralRest,
                                    shape = RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (session.tabs.size > 99) "99+" else session.tabs.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CpdColor.CoralRest,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (session.tabs.size == 1) "1 tab" else "${session.tabs.size} tabs",
                            color = CpdColor.textPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hero Welcome
                    item {
                        Spacer(Modifier.height(CpdSpace.space2x))
                        Text(
                            text = "Your space to explore.",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Normal,
                            color = CpdColor.textPrimary
                        )
                        Spacer(Modifier.height(CpdSpace.space15x))
                        Text(
                            text = "Search anything or visit any website.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CpdColor.textSecondary
                        )
                        Spacer(Modifier.height(CpdSpace.space4x))
                    }

                    // Search Provider Chips (Google, DuckDuckGo, Bing)
                    item {
                        Row(
                            modifier = Modifier.padding(bottom = CpdSpace.space3x),
                            horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)
                        ) {
                            listOf("Google", "DuckDuckGo", "Bing").forEach { engine ->
                                FilterChip(
                                    selected = selectedEngine == engine,
                                    onClick = { selectedEngine = engine },
                                    label = { Text(engine, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandCoralOrange.copy(alpha = 0.15f),
                                        selectedLabelColor = BrandCoralOrange
                                    )
                                )
                            }
                        }
                    }

                    // Search & Omnibox Field
                    item {
                        OutlinedTextField(
                            value = startQuery,
                            onValueChange = { startQuery = it },
                            modifier = Modifier.fillMaxWidth().height(CpdSize.inputHeight),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    "Search with $selectedEngine or type URL",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CpdColor.textDisabled
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = CpdColor.textPrimary),
                            leadingIcon = { Icon(HugeIcons.Search, null, Modifier.size(CpdSize.iconMd), tint = CpdColor.textSecondary) },
                            trailingIcon = {
                                if (startQuery.isNotBlank()) {
                                    IconButton(onClick = { open(startQuery) }) {
                                        Icon(HugeIcons.ArrowForward, "Open", tint = BrandCoralOrange)
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
                                imeAction = androidx.compose.ui.text.input.ImeAction.Go
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onGo = { open(startQuery) }
                            )
                        )
                        Spacer(Modifier.height(CpdSpace.space5x))
                    }

                    // Top Platforms Grid (12 platforms)
                    items(PlatformRegistry.platforms.chunked(4)) { rowPlatforms ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowPlatforms.forEach { platform ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(CpdRadius.radiusLg))
                                        .clickable { open("https://${platform.domains.first()}") }
                                        .padding(vertical = CpdSpace.space25x),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    PlatformIcon(platform.id)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = platform.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CpdColor.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Saved Links Quick Horizontal Carousel
                    if (saved.links.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saved bookmarks",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CpdColor.textSecondary,
                                    fontWeight = FontWeight.Normal
                                )
                                TextButton(onClick = onSavedLinks) {
                                    Text("View all", color = BrandCoralOrange, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                items(saved.links.take(6)) { bookmark ->
                                    Surface(
                                        shape = RoundedCornerShape(CpdRadius.radiusLg),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable { open(bookmark.url) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = CpdSpace.space3x, vertical = CpdSpace.space2x),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(HugeIcons.PushPin, null, Modifier.size(CpdSize.iconSm), tint = BrandCoralOrange)
                                            Spacer(Modifier.width(CpdSpace.space15x))
                                            Text(
                                                text = bookmark.title.ifBlank { BrowserRules.displayHost(bookmark.url) },
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Continue Browsing Button
                    if (currentUrl.isNotBlank()) {
                        item {
                            Spacer(Modifier.height(14.dp))
                            val displayContinueTitle = remember(pageTitle, currentUrl) {
                                val decodedUrl = runCatching { java.net.URLDecoder.decode(currentUrl, "UTF-8") }.getOrDefault(currentUrl)
                                val cleanHost = BrowserRules.displayHost(decodedUrl)
                                when {
                                    pageTitle.isNotBlank() && !pageTitle.startsWith("http", true) && !pageTitle.contains("%") -> pageTitle
                                    cleanHost.isNotBlank() -> cleanHost
                                    else -> decodedUrl.removePrefix("https://").removePrefix("http://")
                                }
                            }
                            TextButton(
                                onClick = {
                                    onStartChanged(false)
                                    webView.onResume()
                                }
                            ) {
                                Icon(HugeIcons.ChevronRight, null, Modifier.size(16.dp), tint = CpdColor.CoralRest)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Continue: $displayContinueTitle",
                                    color = CpdColor.CoralRest,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun browserPageColor() = com.mediadock.app.core.designsystem.theme.CpdColor.bgCanvasDefault

private fun WebView.applyDesktop(desktop: Boolean, context: Context) {
    val mobile = WebSettings.getDefaultUserAgent(context)
    settings.userAgentString = if (desktop) {
        mobile.replace("; wv", "")
            .replace(Regex("\\(Linux; Android[^)]*\\)"), "(X11; Linux x86_64)")
            .replace(" Mobile ", " ")
    } else {
        mobile
    }
    settings.useWideViewPort = desktop
    settings.loadWithOverviewMode = desktop
}
