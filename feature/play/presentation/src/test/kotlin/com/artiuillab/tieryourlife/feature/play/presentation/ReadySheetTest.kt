package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.play.domain.FakeSavedGames
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRankings
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The window at a game's end (artboard 35): what it says, and what each of its buttons does. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReadySheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Four tiers with cards and one without: three are drawn, the fourth counted. */
    private val result = board(
        1L to listOf(1L, 2L),
        2L to listOf(3L),
        3L to emptyList(),
        4L to listOf(4L),
        5L to listOf(5L, 6L),
        POOL to emptyList(),
    )

    private var dismissed = 0
    private var shared = 0
    private var again = 0
    private var shown = 0

    private fun show(rankings: PlayRankings?) {
        composeRule.setContent {
            TierYourLifeTheme {
                ReadyContent(
                    ready = ReadyState(cards = 6, duels = 9, board = result, intoMine = rankings != null),
                    rankings = rankings,
                    onDismiss = { dismissed++ },
                    onShare = { shared++ },
                    onAgain = { again++ },
                )
            }
        }
    }

    @Test
    fun saysWhatTheGameCameTo_drawsTheFirstThreeTiersWithCards_andCountsTheRest() {
        show(rankings = null)

        composeRule.onNodeWithText("Your ranking is ready").assertExists()
        composeRule.onNodeWithText("6 cards placed from 9 duels").assertExists()
        listOf("T1", "T2", "T4").forEach { composeRule.onNodeWithText(it).assertExists() }
        composeRule.onNodeWithText("T3").assertDoesNotExist()
        composeRule.onNodeWithText("T5").assertDoesNotExist()
        composeRule.onNodeWithText("and 1 more tier").assertExists()
        // One's own board: nothing about Rankings.
        composeRule.onNodeWithTag(PlayTestTags.READY_RANKINGS).assertDoesNotExist()
    }

    @Test
    fun eachButton_doesItsOneThing() {
        show(rankings = null)

        composeRule.onNodeWithTag(PlayTestTags.SEE_MY_LIST).performScrollTo().performClick()
        composeRule.onNodeWithTag(PlayTestTags.SHARE_RESULT).performScrollTo().performClick()
        composeRule.onNodeWithTag(PlayTestTags.PLAY_AGAIN).performScrollTo().performClick()

        assertEquals(listOf(1, 1, 1), listOf(dismissed, shared, again))
    }

    @Test
    fun onSomeoneElsesList_aGuestIsOfferedToSignIn_andTheWindowClosesFirst() {
        show(PlayRankings(signedIn = false) { shown++ })

        composeRule.onNodeWithText("Sign in to show it in Rankings").performScrollTo().performClick()

        assertEquals(1, dismissed)
        assertEquals(1, shown)
    }

    @Test
    fun onSomeoneElsesList_signedIn_showsItInRankings() {
        show(PlayRankings(signedIn = true) { shown++ })

        composeRule.onNodeWithText("Show in Rankings").performScrollTo().performClick()

        assertEquals(1, shown)
    }

    @Test
    fun theLastAnswer_opensTheWindow_andSeeMyListClosesPlay() {
        val unranked = board(1L to emptyList(), 2L to emptyList(), POOL to (1L..6L).toList())
        val viewModel = PlayViewModel(FakeSavedGames(), MovingTierRepository(unranked))
        var closed = 0
        composeRule.setContent {
            TierYourLifeTheme {
                PlayLayer(PlayRequest.ownBoard(unranked), open = true, onArranged = { _, _ -> }, onClose = { closed++ }, viewModel = viewModel)
            }
        }
        composeRule.onNodeWithTag(PlayTestTags.START).performScrollTo().performClick()
        composeRule.runOnIdle { while (viewModel.state.value.stage is PlayStage.Dueling) viewModel.duel.pick(first = true) }
        composeRule.waitUntil(5_000) { tagged(PlayTestTags.READY) == 1 }
        assertEquals(0, closed)

        composeRule.onNodeWithTag(PlayTestTags.SEE_MY_LIST).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { tagged(PlayTestTags.READY) == 0 }

        assertEquals(1, closed)
        assertTrue(viewModel.state.value.stage == PlayStage.Closed)
    }

    private fun tagged(tag: String): Int =
        composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes(atLeastOneRootRequired = false).size
}
