package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
}
