package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DuelScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val duel = DuelState(
        kind = GameKind.Hill,
        first = TierItem(1, "Dune", null),
        second = TierItem(2, "Arrival", null),
        number = 1,
        total = 5,
    )

    @Test
    fun tappingACard_picksIt_byItsName() {
        val picks = mutableListOf<Boolean>()
        composeRule.setContent {
            TierYourLifeTheme { DuelScreen(duel = duel, onPick = { picks += it }, onSkip = {}, onUndo = {}, onQuit = {}) }
        }

        composeRule.onNodeWithContentDescription("Pick Arrival").performClick()
        composeRule.mainClock.advanceTimeBy(500)

        assertEquals(listOf(false), picks)
    }

    @Test
    fun aSecondTap_beforeTheNextDuel_countsForNothing() {
        val picks = mutableListOf<Boolean>()
        composeRule.setContent {
            TierYourLifeTheme { DuelScreen(duel = duel, onPick = { picks += it }, onSkip = {}, onUndo = {}, onQuit = {}) }
        }

        composeRule.onNodeWithTag(PlayTestTags.FIRST_CARD).performClick()
        composeRule.onNodeWithTag(PlayTestTags.SECOND_CARD).performClick()
        composeRule.mainClock.advanceTimeBy(500)

        assertEquals(listOf(true), picks)
    }

    @Test
    fun skipAndQuit_doWhatTheySay_andTheFirstDuelHasNothingToUndo() {
        var skipped = 0
        var quit = 0
        composeRule.setContent {
            TierYourLifeTheme { DuelScreen(duel = duel, onPick = {}, onSkip = { skipped++ }, onUndo = {}, onQuit = { quit++ }) }
        }

        composeRule.onNodeWithTag(PlayTestTags.SKIP).performClick()
        composeRule.onNodeWithContentDescription("Quit").performClick()

        composeRule.onNodeWithTag(PlayTestTags.UNDO).assertIsNotEnabled()
        assertEquals(1, skipped)
        assertEquals(1, quit)
    }

    // Sideways the header is gone: Quit and Undo move to the strip, Skip between the cards.
    @Test
    @Config(qualifiers = "w930dp-h411dp-land")
    fun sideways_everyControlIsStillThere_andTheCardsTakeTheHeight() {
        var skipped = 0
        var quit = 0
        var undone = 0
        composeRule.setContent {
            TierYourLifeTheme {
                DuelScreen(duel = duel.copy(canUndo = true), onPick = {}, onSkip = { skipped++ }, onUndo = { undone++ }, onQuit = { quit++ })
            }
        }

        composeRule.onNodeWithTag(PlayTestTags.SKIP).performClick()
        composeRule.onNodeWithTag(PlayTestTags.UNDO).performClick()
        composeRule.onNodeWithContentDescription("Quit").performClick()
        composeRule.onNodeWithText("Duel 1 of 5").assertExists()

        assertEquals(listOf(1, 1, 1), listOf(skipped, undone, quit))
        val card = composeRule.onNodeWithTag(PlayTestTags.FIRST_CARD).getUnclippedBoundsInRoot()
        // 411 less the progress, the stage's margins and a line for the name, with nothing kept back for a header.
        assertEquals(351f, (card.bottom - card.top).value, 1f)
    }

    @Test
    fun theSideMetrics_keepTheCardsShape_andGiveWayToTheWidth() {
        val tall = DuelMetrics.byHeight(width = 800.dp, height = 343.dp)
        assertEquals(315f, tall.height.value, 0.5f)
        assertEquals(224f, tall.width.value, 0.5f)

        val narrow = DuelMetrics.byHeight(width = 500.dp, height = 343.dp)
        assertEquals((500f - 196f - 48f) / 2, narrow.width.value, 0.5f)
        assertEquals(narrow.width.value / 0.711f, narrow.height.value, 0.5f)
    }
}
