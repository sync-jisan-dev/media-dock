package com.mediadock.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mediadock.app.core.designsystem.components.BottomNavItem
import com.mediadock.app.core.designsystem.components.MediaDockBottomBar
import com.mediadock.app.core.designsystem.theme.MediaDockTheme
import com.mediadock.app.feature.navigation.Screen
import com.mediadock.app.feature.navigation.navigateToTopLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TopLevelNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var controller: NavHostController

    @Before fun setUp() {
        compose.setContent {
            MediaDockTheme {
                controller = rememberNavController()
                val entry by controller.currentBackStackEntryAsState()
                Scaffold(bottomBar = {
                    MediaDockBottomBar(entry?.destination?.route, controller::navigateToTopLevel)
                }) { padding ->
                    NavHost(controller, Screen.Home.route, Modifier.padding(padding)) {
                        BottomNavItem.entries.forEach { item ->
                            composable(item.route) {
                                var count by rememberSaveable { mutableStateOf(0) }
                                Button(
                                    onClick = { count++ },
                                    modifier = Modifier.testTag("screen-${item.route}")
                                ) { Text("${item.route}:$count") }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test fun homeWorksAfterEveryTabAndRepeatedSwitches() {
        repeat(3) {
            listOf("Browse", "Queue", "My files", "Me").forEach { label ->
                compose.onNodeWithText(label).performClick().assertIsSelected()
                compose.onNodeWithText("Home").performClick().assertIsSelected()
                compose.onNodeWithTag("screen-home").assertIsDisplayed()
                compose.runOnIdle {
                    assertEquals(Screen.Home.route, controller.currentDestination?.route)
                    assertNull(controller.previousBackStackEntry)
                }
            }
        }
    }

    @Test fun switchingTabsAndReturningHomePreservesEachTabsState() {
        compose.onNodeWithTag("screen-home").performClick()
        compose.onNodeWithText("Browse").performClick()
        compose.onNodeWithTag("screen-explore").performClick()
        compose.onNodeWithText("Queue").performClick()
        compose.onNodeWithText("Browse").performClick()
        compose.onNodeWithText("explore:1").assertIsDisplayed()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("home:1").assertIsDisplayed()
        compose.onNodeWithText("Browse").performClick()
        compose.onNodeWithText("explore:1").assertIsDisplayed()
    }

    @Test fun reselectingTabDoesNotDuplicateItAndBackReturnsHome() {
        compose.onNodeWithText("My files").performClick()
        compose.onNodeWithTag("screen-history").performClick()
        repeat(3) { compose.onNodeWithText("My files").performClick() }
        compose.onNodeWithText("history:1").assertIsDisplayed()
        compose.runOnIdle { controller.popBackStack() }
        compose.onNodeWithTag("screen-home").assertIsDisplayed()
        compose.runOnIdle { assertNull(controller.previousBackStackEntry) }
    }
}
