package com.mediadock.app.feature.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.datastore.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class WorkspaceViewModel @Inject constructor(private val store: WorkspaceStore) : ViewModel() {
    val state = store.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkspaceState())
    fun add(text: String) = viewModelScope.launch { store.addLinks(WorkspaceRules.extractLinks(text).map { SavedLink(it) }) }
    fun importLinks(text: String) = viewModelScope.launch { store.addLinks(WorkspaceRules.importLinks(text)) }
    fun save(url: String, title: String) = viewModelScope.launch { store.addLinks(listOf(SavedLink(url, title))) }
    fun visit(url: String, title: String) = viewModelScope.launch { store.recordPage(url, title) }
    fun pin(url: String) = edit { s -> s.copy(links = s.links.map { if (it.url == url) it.copy(pinned = !it.pinned) else it }) }
    fun renameLink(url: String, title: String) = edit { s -> s.copy(links = s.links.map { if (it.url == url) it.copy(title = title.trim().take(200)) else it }) }
    fun removeLink(url: String) = edit { s -> s.copy(links = s.links.filterNot { it.url == url }) }
    fun clearRecent() = edit { it.copy(recentPages = emptyList()) }
    fun edit(change: (WorkspaceState) -> WorkspaceState) = viewModelScope.launch { store.update(change) }
}
