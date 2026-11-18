package com.mediadock.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.mediadock.app.core.designsystem.theme.MediaDockTheme
import com.mediadock.app.feature.home.HomeLanding
import com.mediadock.app.feature.media.MediaPreparationScreen
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class HomeExperienceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun copiedLinkWaitsForTapAndFavoritePlatformsStayBelowSearch() {
        var requested = ""
        compose.setContent { MediaDockTheme {
            HomeLanding("", null, "https://example.com/video", {}, {}, {}, { requested = it }, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Search or paste a link").assertExists()
        compose.onNodeWithText("Favorite platforms").assertExists()
        compose.runOnIdle { assertEquals("", requested) }
        compose.onNodeWithText("Download this link").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("https://example.com/video", requested) }
        compose.onNodeWithText("Good finds.").assertDoesNotExist()
    }
    @Test fun waitingGameRespondsWithoutPretendingToAdvanceTheDownload() {
        compose.setContent { MediaDockTheme { MediaPreparationScreen(sourceUrl = "https://example.com/video") } }
        compose.onNodeWithText("Finding your media").assertExists()
        compose.onNodeWithContentDescription("Snake game board").assertExists()
        compose.onNodeWithText("Start Snake").performScrollTo().performClick()
        compose.onNodeWithText("Pause Snake").performScrollTo().performClick()
        compose.onNodeWithText("Restart").performScrollTo().performClick()
        compose.onNodeWithText("Start Snake").assertExists()
        compose.onNodeWithText("Finding your media").assertExists()
    }
}
