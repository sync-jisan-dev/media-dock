package com.mediadock.app

import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.core.datastore.*
import com.mediadock.app.feature.history.LibraryRules
import org.junit.Assert.*
import org.junit.Test

class LibraryRulesTest {
    private val files = listOf(
        HistoryEntity(1, "a", "https://example.com/one", "Zebra", "video", "mp4", "content://one", 100, 10),
        HistoryEntity(2, "b", "https://example.com/two", "Alpha", "audio", "mp3", "content://two", 500, 30),
        HistoryEntity(3, "c", "https://example.com/three", "Beta", "image", "jpg", "content://three", 50, 20)
    )
    @Test fun dateSortingWorksBothDirections() {
        assertEquals(listOf(2L, 3L, 1L), LibraryRules.select(files, WorkspaceState()).map { it.id })
        assertEquals(listOf(1L, 3L, 2L), LibraryRules.select(files, WorkspaceState(librarySort = "Oldest first")).map { it.id })
    }
    @Test fun titleSortUsesEditedTitles() {
        val state = WorkspaceState(librarySort = "Title A–Z", titles = mapOf("1" to "Aardvark"))
        assertEquals(listOf(1L, 2L, 3L), LibraryRules.select(files, state).map { it.id })
    }
    @Test fun sizeSortingWorksBothDirections() {
        assertEquals(listOf(2L, 1L, 3L), LibraryRules.select(files, WorkspaceState(librarySort = "Largest first")).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), LibraryRules.select(files, WorkspaceState(librarySort = "Smallest first")).map { it.id })
    }
    @Test fun combinesFavoritesTypeCollectionAndSearch() {
        val state = WorkspaceState(favorites = setOf(1, 2), notes = mapOf("1" to "Kotlin lesson"), collections = listOf(MediaCollection("learn", "Learn", setOf(1, 3))))
        assertEquals(listOf(1L), LibraryRules.select(files, state, "kotlin", "video", true, "learn").map { it.id })
        assertTrue(LibraryRules.select(files, state, "kotlin", "audio", true, "learn").isEmpty())
    }
    @Test fun searchesEditedTitlesNotesFormatsAndSources() {
        val state = WorkspaceState(titles = mapOf("1" to "Travel"), notes = mapOf("3" to "Design reference"))
        assertEquals(1L, LibraryRules.select(files, state, "travel").single().id)
        assertEquals(3L, LibraryRules.select(files, state, "reference").single().id)
        assertEquals(2L, LibraryRules.select(files, state, "mp3").single().id)
        assertEquals(3L, LibraryRules.select(files, state, "/three").single().id)
    }
    @Test fun unknownCollectionNeverShowsUnrelatedFiles() { assertTrue(LibraryRules.select(files, WorkspaceState(), collection = "missing").isEmpty()) }
}
