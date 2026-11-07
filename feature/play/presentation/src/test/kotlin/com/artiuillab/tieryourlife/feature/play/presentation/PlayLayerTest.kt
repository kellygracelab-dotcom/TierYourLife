package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The picker is a window of its own over the board, its scrim with it. Taken
 * away in the frame the duels are first drawn, it can leave the window before
 * they reach the screen, and the bare board shows between the two.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayLayerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val unranked = board(1L to emptyList(), 2L to emptyList(), POOL to (1L..6L).toList())
    private val request = PlayRequest.ownBoard(unranked)

    @Test
    fun startingAGame_keepsThePickerUp_untilTheDuelsAreDrawnUnderIt() {
        val viewModel = PlayViewModel(FakeSavedGames(), MovingTierRepository(unranked))
        composeRule.setContent { TierYourLifeTheme { PlayLayer(request, open = true, onArranged = { _, _ -> }, onClose = {}, viewModel = viewModel) } }
        composeRule.onNodeWithTag(PlayTestTags.START).assertExists()

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(PlayTestTags.START).performClick()
        composeRule.mainClock.advanceTimeByFrame()

        assertEquals(1, tagged(PlayTestTags.DUEL))
        assertEquals(1, tagged(PlayTestTags.PICKER))

        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()

        assertEquals(1, tagged(PlayTestTags.DUEL))
        assertEquals(0, tagged(PlayTestTags.PICKER))
    }

    private fun tagged(tag: String): Int =
        composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes(atLeastOneRootRequired = false).size
}
