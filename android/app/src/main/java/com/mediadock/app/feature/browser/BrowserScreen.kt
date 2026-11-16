package com.mediadock.app.feature.browser

import com.mediadock.app.core.designsystem.icons.HugeIcons

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import com.mediadock.app.domain.model.PlatformRegistry
import com.mediadock.app.feature.home.PlatformIcon
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(initialUrl: String, onDownload: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var address by rememberSaveable { mutableStateOf(initialUrl) }
    var currentUrl by rememberSaveable { mutableStateOf("") }
    var progress by remember { mutableStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var browsing by rememberSaveable { mutableStateOf(initialUrl.isNotBlank()) }
    val downloadCallback by rememberUpdatedState(onDownload)
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, value: Int) { progress = value }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return request.url.scheme !in listOf("http", "https")
                }
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    currentUrl = url
                    address = url
                }
                override fun onPageFinished(view: WebView, url: String) {
                    currentUrl = url
                    canGoBack = view.canGoBack()
                    canGoForward = view.canGoForward()
                }
            }
            setDownloadListener { url, _, _, _, _ -> downloadCallback(url) }
        }
    }
    fun open(value: String) {
        if (value.isBlank()) return
        val trimmed = value.trim()
        val target = when {
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            !trimmed.contains(' ') && trimmed.contains('.') -> "https://$trimmed"
            else -> "https://duckduckgo.com/?q=${URLEncoder.encode(trimmed, "UTF-8")}"
        }
        browsing = true
        currentUrl = target
        webView.loadUrl(target)
    }
    LaunchedEffect(Unit) { if (browsing) open(currentUrl.ifBlank { initialUrl }) }
    DisposableEffect(webView) { onDispose { webView.stopLoading(); webView.destroy() } }
    BackHandler(browsing) {
        if (webView.canGoBack()) webView.goBack() else { webView.stopLoading(); browsing = false; currentUrl = ""; address = "" }
    }
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            address, { address = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search or enter web address") }, singleLine = true,
            leadingIcon = { Icon(HugeIcons.Search, null) },
            trailingIcon = { IconButton(onClick = { open(address) }) { Icon(HugeIcons.ArrowForward, "Open address") } },
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { open(address) })
        )
        if (progress < 100 && browsing) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        if (browsing) {
            AndroidView(factory = { webView }, modifier = Modifier.weight(1f).fillMaxWidth())
            Surface(shadowElevation = 4.dp) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { webView.goBack() }, enabled = canGoBack) { Icon(HugeIcons.ArrowBack, "Previous page") }
                    IconButton(onClick = { webView.goForward() }, enabled = canGoForward) { Icon(HugeIcons.ArrowForward, "Next page") }
                    IconButton(onClick = { webView.reload() }) { Icon(HugeIcons.Refresh, "Reload page") }
                    Button(onClick = { downloadCallback(currentUrl) }, enabled = Uri.parse(currentUrl).scheme in listOf("http", "https")) {
                        Icon(HugeIcons.Download, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Download")
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Discover your next favorite", style = MaterialTheme.typography.headlineSmall)
                    Text("Browse a platform, open a video, then tap Download.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }
                items(PlatformRegistry.platforms, key = { it.id }) { platform ->
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clickable { open("https://${platform.domains.first()}") }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            PlatformIcon(platform.id)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(platform.name, style = MaterialTheme.typography.titleMedium)
                                Text(platform.supportedTypes.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(HugeIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
