package com.mediadock.app.feature.browser

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.datastore.BrowserTab
import com.mediadock.app.core.datastore.WorkspaceStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class BrowserSession(val tabs: List<BrowserTab> = emptyList(), val activeId: String = "", val ready: Boolean = false) {
    val active: BrowserTab? get() = tabs.find { it.id == activeId }
}

@HiltViewModel class BrowserSessionViewModel @Inject constructor(private val store: WorkspaceStore) : ViewModel() {
    val session = MutableStateFlow(BrowserSession())
    private val bundles = mutableMapOf<String, Bundle>()
    private val initLock = Mutex()
    suspend fun initialize(initialUrl: String) = initLock.withLock {
        if (session.value.ready) return@withLock
        val saved = store.state.first()
        val tabs = saved.browserTabs.take(BrowserRules.MAX_TABS).ifEmpty { listOf(BrowserTab()) }
        session.value = BrowserSession(tabs, saved.activeBrowserTab?.takeIf { id -> tabs.any { it.id == id } } ?: tabs.first().id, true)
        if (initialUrl.isNotBlank()) newTab(BrowserRules.destination(initialUrl)) else persist()
    }
    private fun persist() {
        val snapshot = session.value
        viewModelScope.launch { store.update { it.copy(browserTabs = snapshot.tabs, activeBrowserTab = snapshot.activeId) } }
    }
    fun updateTab(id: String, change: (BrowserTab) -> BrowserTab) {
        session.value = session.value.copy(tabs = session.value.tabs.map { if (it.id == id) change(it) else it })
        persist()
    }
    fun navigate(url: String) { bundles.remove(session.value.activeId); updateTab(session.value.activeId) { it.copy(url = url, scrollY = 0) } }
    fun newTab(url: String = ""): Boolean {
        val current = session.value
        val blank = current.active?.takeIf { it.url.isBlank() && url.isNotBlank() }
        if (blank != null) { navigate(url); return true }
        if (current.tabs.size >= BrowserRules.MAX_TABS) return false
        val tab = BrowserTab(url = url)
        session.value = current.copy(tabs = current.tabs + tab, activeId = tab.id); persist(); return true
    }
    fun selectTab(id: String) { if (session.value.tabs.any { it.id == id }) { session.value = session.value.copy(activeId = id); persist() } }
    fun closeTab(id: String) {
        val current = session.value; val index = current.tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        bundles.remove(id)
        val remaining = current.tabs.filterNot { it.id == id }.ifEmpty { listOf(BrowserTab()) }
        session.value = current.copy(tabs = remaining, activeId = if (current.activeId == id) remaining[index.coerceAtMost(remaining.lastIndex)].id else current.activeId)
        persist()
    }
    fun closeAllTabs() {
        bundles.clear()
        val fresh = BrowserTab()
        session.value = session.value.copy(tabs = listOf(fresh), activeId = fresh.id)
        persist()
    }
    fun saveWebState(id: String, bundle: Bundle, scrollY: Int) {
        if (session.value.tabs.none { it.id == id }) return
        bundles[id] = bundle
        updateTab(id) { it.copy(scrollY = scrollY.coerceAtLeast(0)) }
    }
    fun restoreWebState(id: String): Bundle? = bundles[id]
}
