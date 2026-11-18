package com.mediadock.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.workspaceData by preferencesDataStore("mediadock_workspace")

@Serializable data class SavedLink(
    val url: String, val title: String = "", val pinned: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
@Serializable data class MediaCollection(
    val id: String = UUID.randomUUID().toString(), val name: String,
    val fileIds: Set<Long> = emptySet()
)
@Serializable data class BrowserTab(
    val id: String = UUID.randomUUID().toString(), val url: String = "",
    val title: String = "New tab", val desktop: Boolean = false,
    val scrollY: Int = 0, val textZoom: Int = 100
)
@Serializable data class WorkspaceState(
    val links: List<SavedLink> = emptyList(),
    val recentPages: List<SavedLink> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val titles: Map<String, String> = emptyMap(),
    val notes: Map<String, String> = emptyMap(),
    val collections: List<MediaCollection> = emptyList(),
    val librarySort: String = "Newest first",
    val grid: Boolean = false,
    val compact: Boolean = false,
    val browserTabs: List<BrowserTab> = emptyList(),
    val activeBrowserTab: String? = null
)

object WorkspaceRules {
    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    fun validUrl(value: String): Boolean = runCatching {
        val uri = URI(value.trim())
        uri.scheme?.lowercase() in setOf("https", "http") && !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)
    fun extractLinks(text: String): List<String> = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
        .findAll(text).map { it.value.trimEnd('.', ',', ';', ')', ']', '}') }
        .filter(::validUrl).distinct().take(500).toList()
    fun mergeLinks(existing: List<SavedLink>, incoming: List<SavedLink>): List<SavedLink> =
        (existing + incoming.filter { validUrl(it.url) }.map { it.copy(url = it.url.trim(), title = it.title.take(200)) })
            .distinctBy { it.url }.take(2000)
    fun importLinks(text: String): List<SavedLink> {
        require(text.length <= 524288) { "Choose a JSON file smaller than 512 KB." }
        val links = json.decodeFromString<List<SavedLink>>(text)
        require(links.size <= 500) { "Import at most 500 links at a time." }
        require(links.all { validUrl(it.url) }) { "The file contains an invalid web link." }
        return links
    }
}

@Singleton class WorkspaceStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("workspace_v1")
    private fun decode(value: String?) = runCatching {
        WorkspaceRules.json.decodeFromString<WorkspaceState>(value ?: "{}")
    }.getOrDefault(WorkspaceState())
    val state = context.workspaceData.data.map { decode(it[key]) }
    suspend fun update(change: (WorkspaceState) -> WorkspaceState) {
        context.workspaceData.edit { it[key] = WorkspaceRules.json.encodeToString(change(decode(it[key]))) }
    }
    suspend fun addLinks(links: List<SavedLink>) = update { it.copy(links = WorkspaceRules.mergeLinks(it.links, links)) }
    suspend fun recordPage(url: String, title: String) {
        if (!WorkspaceRules.validUrl(url)) return
        update { it.copy(recentPages = (listOf(SavedLink(url, title.take(200))) + it.recentPages.filterNot { page -> page.url == url }).take(100)) }
    }
}
