package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.ui.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.core.theme.ui.HomeBarTestTags
import com.artiuillab.tieryourlife.core.theme.ui.ReplacingSnackbar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Removed: … · Undo": a new one takes the old one's place at once, and a swipe sends it away. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReplacingSnackbarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val results = mutableListOf<Pair<String, SnackbarResult>>()
    private lateinit var snackbar: ReplacingSnackbar

    private fun show() {
        composeRule.setContent {
            val host = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            snackbar = remember { ReplacingSnackbar(host, scope) }
            TierYourLifeTheme { DeletedItemSnackbarHost(host) }
        }
    }

    private fun removed(title: String) = composeRule.runOnIdle {
        snackbar.show("Removed $title", "Undo") { results += title to it }
    }

    @Test
    fun aSecondRemoval_replacesTheFirst_whichEndsWithoutUndo() {
        show()
        removed("Dune")
        composeRule.onNodeWithText("Removed Dune").assertExists()

        removed("Arrival")

        composeRule.onNodeWithText("Removed Arrival").assertExists()
        composeRule.onNodeWithText("Removed Dune").assertDoesNotExist()
        assertEquals(listOf("Dune" to SnackbarResult.Dismissed), results)

        composeRule.onNodeWithText("Undo").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("Dune" to SnackbarResult.Dismissed, "Arrival" to SnackbarResult.ActionPerformed), results)
        }
    }

    @Test
    fun aSwipe_sendsItAway() {
        show()
        removed("Dune")

        composeRule.onNodeWithTag(HomeBarTestTags.DELETED_ITEM_SNACKBAR).performTouchInput { swipeRight() }

        composeRule.onNodeWithText("Removed Dune").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(listOf("Dune" to SnackbarResult.Dismissed), results) }
    }

    @Test
    fun onItsOwn_itGoesAfterAShortWhile() {
        show()
        composeRule.mainClock.autoAdvance = false
        removed("Dune")
        composeRule.mainClock.advanceTimeBy(SHORT_MS - 500)
        composeRule.onNodeWithText("Removed Dune").assertExists()

        composeRule.mainClock.advanceTimeBy(1_500)

        composeRule.onNodeWithText("Removed Dune").assertDoesNotExist()
        assertEquals(listOf("Dune" to SnackbarResult.Dismissed), results)
    }

    private companion object {
        const val SHORT_MS = 4_000L
    }
}
