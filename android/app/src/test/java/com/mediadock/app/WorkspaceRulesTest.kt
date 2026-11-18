package com.mediadock.app

import com.mediadock.app.core.datastore.*
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class WorkspaceRulesTest {
    @Test fun extractsAndDeduplicatesWithoutChangingSignedQueries() {
        val text = "Save https://example.com/a?token=a+b%2Fc and https://example.com/a?token=a+b%2Fc\nhttps://youtu.be/test."
        assertEquals(listOf("https://example.com/a?token=a+b%2Fc", "https://youtu.be/test"), WorkspaceRules.extractLinks(text))
    }
    @Test fun rejectsInvalidLinksAndEmbeddedCredentials() {
        listOf("ftp://example.com/test", "https://", "https://user:secret@example.com", "not a URL").forEach { assertFalse(it, WorkspaceRules.validUrl(it)) }
        assertTrue(WorkspaceRules.validUrl("https://example.com:8443/a?q=x"))
    }
    @Test fun importExportRoundTripPreservesTitlesPinsAndDates() {
        val links = listOf(SavedLink("https://example.com/video", "A title", true, 42))
        assertEquals(links, WorkspaceRules.importLinks(WorkspaceRules.json.encodeToString(links)))
    }
    @Test fun mergingPreservesExistingUserMetadata() {
        val saved = SavedLink("https://example.com/a", "Edited", true)
        val merged = WorkspaceRules.mergeLinks(listOf(saved), listOf(saved.copy(title = "Other", pinned = false), SavedLink("https://example.com/b")))
        assertEquals(2, merged.size); assertEquals(saved, merged.first())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOversizedImport() { WorkspaceRules.importLinks(" ".repeat(524289)) }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidLinkInsideImport() { WorkspaceRules.importLinks("""[{"url":"not-a-web-link"}]""") }
    @Test(expected = Exception::class) fun rejectsMalformedImport() { WorkspaceRules.importLinks("not json") }
    @Test fun workspaceRoundTripKeepsCollectionsNotesAndDisplayPreferences() {
        val state = WorkspaceState(favorites = setOf(2), notes = mapOf("2" to "Watch later"), collections = listOf(MediaCollection(name = "Learn", fileIds = setOf(2))), librarySort = "Largest first", grid = true, compact = true)
        assertEquals(state, WorkspaceRules.json.decodeFromString<WorkspaceState>(WorkspaceRules.json.encodeToString(state)))
    }
}
