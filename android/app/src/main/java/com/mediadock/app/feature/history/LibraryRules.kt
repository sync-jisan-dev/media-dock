package com.mediadock.app.feature.history

import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.core.datastore.WorkspaceState

object LibraryRules {
    val sorts = listOf("Newest first", "Oldest first", "Title A–Z", "Largest first", "Smallest first")
    fun title(file: HistoryEntity, state: WorkspaceState) = state.titles[file.id.toString()] ?: file.title
    fun select(files: List<HistoryEntity>, state: WorkspaceState, query: String = "", type: String = "ALL", favorites: Boolean = false, collection: String? = null): List<HistoryEntity> {
        val ids = state.collections.find { it.id == collection }?.fileIds
        val filtered = files.filter { file ->
            (type == "ALL" || file.mediaType.equals(type, true)) &&
                (!favorites || file.id in state.favorites) &&
                (collection == null || (ids != null && file.id in ids)) &&
                (query.isBlank() || listOf(title(file, state), file.sourceUrl, state.notes[file.id.toString()].orEmpty(), file.format).any { it.contains(query.trim(), true) })
        }
        return when (state.librarySort) {
            "Oldest first" -> filtered.sortedBy { it.downloadedAt }
            "Title A–Z" -> filtered.sortedBy { title(it, state).lowercase(java.util.Locale.ROOT) }
            "Largest first" -> filtered.sortedByDescending { it.fileSizeBytes }
            "Smallest first" -> filtered.sortedBy { it.fileSizeBytes }
            else -> filtered.sortedByDescending { it.downloadedAt }
        }
    }
}
