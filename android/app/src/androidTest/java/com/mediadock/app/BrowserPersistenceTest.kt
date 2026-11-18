package com.mediadock.app

import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.mediadock.app.core.datastore.*
import com.mediadock.app.core.designsystem.theme.MediaDockTheme
import com.mediadock.app.feature.browser.*
import com.mediadock.app.feature.workspace.WorkspaceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.Assert.*
import java.net.ServerSocket
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class BrowserPersistenceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var store: WorkspaceStore
    private lateinit var browser: BrowserSessionViewModel
    private lateinit var workspace: WorkspaceViewModel
    private lateinit var fixture: BrowserFixture

    @Before fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        store = WorkspaceStore(context)
        runBlocking { store.update { WorkspaceState() } }
        browser = BrowserSessionViewModel(store)
        workspace = WorkspaceViewModel(store)
        fixture = BrowserFixture()
        runBlocking { browser.initialize(fixture.origin + "/seed") }
    }
    @After fun after() { runBlocking { store.update { WorkspaceState() } }; fixture.close() }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) findWebView(view.getChildAt(index))?.let { return it }
        return null
    }
    private fun webView(): WebView? {
        val result = AtomicReference<WebView?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result.set(findWebView(compose.activity.window.decorView)) }
        return result.get()
    }
    private fun script(code: String): String {
        val result = AtomicReference("")
        val ready = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = findWebView(compose.activity.window.decorView)
            if (view == null) ready.countDown()
            else view.evaluateJavascript(code) { result.set(it); ready.countDown() }
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS))
        return result.get()
    }
    private fun waitForTitle(title: String) { compose.waitUntil(15000) { webView() != null && script("document.title") == "\"$title\"" } }

    @Test fun cookiesWebsiteStorageAndTabsSurviveWebViewRecreation() {
        compose.setContent { MediaDockTheme { BrowserScreen("", {}, workspace = workspace, startOnEntry = false, browser = browser) } }
        waitForTitle("Seed ready")
        assertEquals("\"kept\"", script("localStorage.getItem('md-fixture')"))
        compose.runOnIdle { assertTrue(browser.newTab(fixture.origin + "/check")) }
        waitForTitle("Check ready")
        assertEquals("true", script("document.cookie.includes('md_fixture=remembered') && localStorage.getItem('md-fixture') === 'kept'"))
        val persisted = runBlocking { withTimeout(5000) { store.state.first { it.browserTabs.size == 2 && it.browserTabs.any { tab -> tab.url.endsWith("/check") } } } }
        val restored = BrowserSessionViewModel(store)
        runBlocking { restored.initialize("") }
        assertEquals(persisted.activeBrowserTab, restored.session.value.activeId)
        assertEquals(2, restored.session.value.tabs.size)
        compose.runOnIdle { CookieManager.getInstance().flush(); assertTrue(CookieManager.getInstance().getCookie(fixture.origin).contains("md_fixture=remembered")) }
    }

    @Test fun switchingAwayAndBackRestoresBackForwardNavigation() {
        var visible by mutableStateOf(true)
        compose.setContent { MediaDockTheme { if (visible) BrowserScreen("", {}, workspace = workspace, startOnEntry = false, browser = browser) else Text("Away") } }
        waitForTitle("Seed ready")
        compose.runOnIdle { requireNotNull(findWebView(compose.activity.window.decorView)).loadUrl(fixture.origin + "/check") }
        waitForTitle("Check ready")
        compose.runOnIdle { visible = false }; compose.waitForIdle()
        compose.runOnIdle { visible = true }
        waitForTitle("Check ready")
        compose.runOnIdle { val view = requireNotNull(findWebView(compose.activity.window.decorView)); assertTrue(view.canGoBack()); view.goBack() }
        waitForTitle("Seed ready")
        compose.runOnIdle { val view = requireNotNull(findWebView(compose.activity.window.decorView)); assertTrue(view.canGoForward()); view.goForward() }
        waitForTitle("Check ready")
    }
}

private class BrowserFixture : AutoCloseable {
    private val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    val origin = "http://127.0.0.1:${server.localPort}"
    init { thread(isDaemon = true, name = "MediaDock browser fixture") {
        while (!server.isClosed) runCatching { server.accept().use { socket ->
            val input = socket.getInputStream().bufferedReader()
            val request = input.readLine().orEmpty()
            while (!input.readLine().isNullOrBlank()) { }
            val seed = request.contains("/seed")
            val body = (if (seed) "<html><head><title>Seed ready</title></head><body><script>localStorage.setItem('md-fixture','kept')</script>Seed page</body></html>" else "<html><head><title>Check ready</title></head><body>Check page</body></html>").toByteArray()
            val cookie = if (seed) "Set-Cookie: md_fixture=remembered; Max-Age=3600; Path=/\r\n" else ""
            socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\n" + cookie + "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray())
            socket.getOutputStream().write(body)
        } }
    } }
    override fun close() { server.close() }
}
