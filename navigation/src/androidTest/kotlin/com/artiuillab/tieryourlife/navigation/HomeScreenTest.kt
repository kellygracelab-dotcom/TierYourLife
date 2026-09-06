package com.artiuillab.tieryourlife.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The tabs between the two halves of the home screen, and who draws them. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun opensOnYourLists_andTheTabsSwitchToTheCommunity() {
        setHome(startOnCommunity = false, shape = WindowShape.Narrow)

        composeRule.onNodeWithText("mine").assertIsDisplayed()
        composeRule.onNodeWithTag("home_tab_community").performClick()

        composeRule.onNodeWithText("community").assertIsDisplayed()
        composeRule.onNodeWithText("mine").assertDoesNotExist()
    }

    @Test
    fun theRouteCanOpenOnTheCommunity() {
        setHome(startOnCommunity = true, shape = WindowShape.Narrow)

        composeRule.onNodeWithText("community").assertIsDisplayed()
    }

    // The rail navigates here again to switch tabs, so a change in the route
    // has to move the tab even though the screen is already up.
    @Test
    fun theRouteChangingUnderTheScreen_switchesTheTab() {
        var startOnCommunity by mutableStateOf(false)
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalWindowShape provides WindowShape.Wide) {
                    HomeScreen(
                        startOnCommunity = startOnCommunity,
                        mine = { _, _ -> Text("mine") },
                        community = { Text("community") },
                    )
                }
            }
        }
        composeRule.onNodeWithText("mine").assertIsDisplayed()

        composeRule.runOnUiThread { startOnCommunity = true }

        composeRule.onNodeWithText("community").assertIsDisplayed()
    }

    // Coming back from a community list rebuilds this screen from saved state
    // with the route still saying "lists". The tab somebody chose has to win
    // over the flag they arrived with.
    @Test
    fun theChosenTab_survivesBeingRebuiltFromSavedState() {
        val restorer = StateRestorationTester(composeRule)
        restorer.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalWindowShape provides WindowShape.Narrow) {
                    HomeScreen(
                        startOnCommunity = false,
                        mine = { tabs, _ ->
                            tabs()
                            Text("mine")
                        },
                        community = { tabs ->
                            tabs()
                            Text("community")
                        },
                    )
                }
            }
        }
        composeRule.onNodeWithTag("home_tab_community").performClick()
        composeRule.onNodeWithText("community").assertIsDisplayed()

        restorer.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("community").assertIsDisplayed()
    }

    // With a rail beside the content, tabs would be a second way to the same places.
    @Test
    fun onAWideWindow_noTabsAreDrawn() {
        setHome(startOnCommunity = false, shape = WindowShape.Wide)

        composeRule.onNodeWithText("mine").assertIsDisplayed()
        composeRule.onNodeWithTag("home_tab_community").assertDoesNotExist()
    }

    // A tab tap re-enters the half's composition; the token it is handed
    // must not change, or the half would take the tap for an arrival.
    @Test
    fun switchingTabs_handsTheSameArrivalBack() {
        val arrivals = mutableListOf<Any?>()
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalWindowShape provides WindowShape.Narrow) {
                    HomeScreen(
                        startOnCommunity = false,
                        mine = { tabs, arrival ->
                            tabs()
                            arrivals += arrival
                            Text("mine")
                        },
                        community = { tabs ->
                            tabs()
                            Text("community")
                        },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val first = arrivals.last()

        composeRule.onNodeWithTag("home_tab_community").performClick()
        composeRule.onNodeWithTag("home_tab_mine").performClick()
        composeRule.onNodeWithText("mine").assertIsDisplayed()

        composeRule.runOnIdle { assertEquals(first, arrivals.last()) }
    }

    private fun setHome(startOnCommunity: Boolean, shape: WindowShape) {
        composeRule.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalWindowShape provides shape) {
                    HomeScreen(
                        startOnCommunity = startOnCommunity,
                        // Each half draws the tabs it was handed, as the real ones do.
                        mine = { tabs, _ ->
                            tabs()
                            Text("mine")
                        },
                        community = { tabs ->
                            tabs()
                            Text("community")
                        },
                    )
                }
            }
        }
    }
}
