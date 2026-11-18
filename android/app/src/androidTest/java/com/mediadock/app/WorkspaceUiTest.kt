package com.mediadock.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.core.datastore.*
import com.mediadock.app.core.designsystem.theme.MediaDockTheme
import com.mediadock.app.data.remote.dto.CreateDownloadRequestDto
import com.mediadock.app.domain.model.ActiveDownload
import com.mediadock.app.domain.repository.DownloadRepository
import com.mediadock.app.domain.usecase.GetHistoryUseCase
import com.mediadock.app.feature.history.*
import com.mediadock.app.feature.workspace.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class WorkspaceUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var store: WorkspaceStore
    private lateinit var workspace: WorkspaceViewModel
    @Before fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug")) { "UI fixtures must never use the installed release app" }
        store = WorkspaceStore(context)
        runBlocking { store.update { WorkspaceState() } }
        workspace = WorkspaceViewModel(store)
    }
    @After fun after() { runBlocking { store.update { WorkspaceState() } } }

    @Test fun addsMultipleLinksDeduplicatesAndOpensTheRequestedDownload() {
        var opened = ""
        compose.setContent { MediaDockTheme { SavedLinksScreen({}, { opened = it }, workspace) } }
        compose.onNodeWithText("Add links").performClick()
        compose.onNodeWithText("https://…").performTextInput("https://example.com/one\nhttps://example.com/two\nhttps://example.com/one")
        compose.onNodeWithText("2 valid links").assertIsDisplayed()
        compose.onNodeWithText("Save links").performClick()
        compose.waitUntil(5000) { workspace.state.value.links.size == 2 }
        compose.onAllNodesWithText("Download").onFirst().performScrollTo().performClick()
        compose.runOnIdle { assertEquals("https://example.com/one", opened) }
        assertEquals(2, runBlocking { store.state.first() }.links.size)
    }

    @Test fun savedLinksCanBePinnedRenamedAndSearched() {
        runBlocking { store.addLinks(listOf(SavedLink("https://example.com/one", "First"), SavedLink("https://example.com/two", "Second"))) }
        compose.setContent { MediaDockTheme { SavedLinksScreen({}, {}, workspace) } }
        compose.onAllNodesWithContentDescription("Pin link").onFirst().performScrollTo().performClick()
        compose.waitUntil(5000) { workspace.state.value.links.any { it.pinned } }
        compose.onAllNodesWithContentDescription("Link actions").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Edit title").performClick()
        compose.onAllNodes(hasSetTextAction()).onLast().performTextReplacement("Learning")
        compose.onNodeWithText("Save", useUnmergedTree = true).performClick()
        compose.waitUntil(5000) { workspace.state.value.links.any { it.title == "Learning" } }
        compose.onNodeWithText("Search titles or links").performScrollTo().performTextInput("Learning")
        compose.onAllNodesWithText("Learning").assertCountEquals(2)
        compose.onNodeWithText("Second").assertDoesNotExist()
    }

    @Test fun libraryCreatesAndRenamesCollectionsAndFiltersFavorites() {
        val repo = FixtureDownloads()
        runBlocking { store.update { it.copy(favorites = setOf(1)) } }
        compose.setContent { MediaDockTheme { HistoryScreen(HistoryViewModel(GetHistoryUseCase(repo)), workspace = workspace) } }
        compose.onNodeWithText("Favorites").performClick()
        compose.onNodeWithText("First video").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Second audio").assertDoesNotExist()
        compose.onNodeWithText("New collection").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Study")
        compose.onNodeWithText("Save", useUnmergedTree = true).performClick()
        compose.waitUntil(5000) { workspace.state.value.collections.size == 1 }
        compose.onNodeWithText("Study").performScrollTo().performClick()
        compose.onNodeWithText("Rename").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction()).onLast().performTextReplacement("Learning")
        compose.onNodeWithText("Save", useUnmergedTree = true).performClick()
        compose.waitUntil(5000) { workspace.state.value.collections.single().name == "Learning" }
        assertEquals(2, repo.files.value.size)
    }
}

private class FixtureDownloads : DownloadRepository {
    val files = MutableStateFlow(listOf(
        HistoryEntity(1, "fixture-one", "https://example.com/one", "First video", "video", "mp4", "content://fixture/one", 100, 10),
        HistoryEntity(2, "fixture-two", "https://example.com/two", "Second audio", "audio", "mp3", "content://fixture/two", 200, 20)
    ))
    override fun getHistory() = files
    override fun searchHistory(query: String) = files.map { it.filter { file -> file.title.contains(query, true) } }
    override suspend fun deleteHistory(id: Long) { files.value = files.value.filterNot { it.id == id } }
    override suspend fun clearHistory() { files.value = emptyList() }
    override suspend fun restoreHistory(items: List<HistoryEntity>) { files.value = (files.value + items).distinctBy { it.id } }
    override fun getActiveDownloads() = flowOf(emptyList<ActiveDownload>())
    override fun getDownload(jobId: String) = flowOf<ActiveDownload?>(null)
    override fun getActiveDownloadsCount() = flowOf(0)
    override suspend fun pauseDownload(jobId: String) {}
    override suspend fun resumeDownload(jobId: String) {}
    override suspend fun cancelDownload(jobId: String) {}
    override suspend fun deleteDownload(jobId: String) {}
    override suspend fun enqueueDownload(request: CreateDownloadRequestDto, title: String, thumbnail: String?) = Resource.Error("Not used by this UI fixture")
    override suspend fun enqueueBatch(urls: List<String>, kind: String, quality: String?, format: String?) = Resource.Error("Not used by this UI fixture")
    override suspend fun enqueueImageDownload(url: String, title: String?, format: String?) = Resource.Error("Not used by this UI fixture")
}
