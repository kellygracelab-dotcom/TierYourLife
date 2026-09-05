package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The cover shows what somebody already made, in the mode they chose, and refuses everything else in one sentence. */
@RunWith(AndroidJUnit4::class)
class CoverBoardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aWrappedBoard_showsItsTiersAsRowsOfCards() {
        setBoard(board(TierListDisplayMode.WRAP))

        composeRule.onNodeWithTag(CoverTestTags.BOARD).assertIsDisplayed()
        // Letters no card's title starts with: a tile shows its first
        // character, so "A" here would mean the band or "Arrival".
        composeRule.onNodeWithText("S").assertIsDisplayed()
        composeRule.onNodeWithText("B").assertIsDisplayed()
    }

    // Captions are the first thing to cost a row its cards at this width, and
    // the letter already is the tier.
    @Test
    fun aWrappedBoard_dropsTheTierCaptions() {
        setBoard(board(TierListDisplayMode.WRAP))

        composeRule.onNodeWithText("Masterpiece").assertDoesNotExist()
    }

    // The board is shown in the mode its owner chose. Squashing a ranked board
    // into coloured strips would be answering a question nobody asked.
    @Test
    fun aRankedBoard_becomesANumberedColumn() {
        setBoard(board(TierListDisplayMode.FLAT_RANKED))

        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("Arrival").assertIsDisplayed()
    }

    // Numbered across the whole board rather than within each tier: the point
    // of the mode is the overall order.
    @Test
    fun aRankedBoard_countsStraightThroughTheTiers() {
        setBoard(board(TierListDisplayMode.FLAT_RANKED))

        // Two in S, so the first card of A is third overall.
        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun reachingForAnythingElse_saysWhatTheCoverCannotDo() {
        var explained = 0
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = true) {
                UnfoldToRank(onDismiss = { explained++ })
            }
        }

        composeRule.onNodeWithTag(CoverTestTags.UNFOLD).assertIsDisplayed()
        composeRule.onNodeWithTag(CoverTestTags.UNFOLD).performClick()

        composeRule.runOnIdle { assertEquals(1, explained) }
    }

    private fun setBoard(board: TierList) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = true) {
                CoverBoard(boards = listOf(board))
            }
        }
    }

    private fun board(mode: TierListDisplayMode) = TierList(
        id = 1,
        title = "Sci-fi films",
        displayMode = mode,
        tiers = listOf(
            tier(1, "S", "Masterpiece", listOf("Arrival", "Dune")),
            tier(2, "B", "Great", listOf("Moonlight")),
        ),
    )

    private fun tier(id: Long, label: String, caption: String, titles: List<String>) = Tier(
        id = id,
        label = label,
        caption = caption,
        colorLight = "#B03A32",
        colorDark = "#F1948C",
        items = titles.mapIndexed { index, title ->
            TierItem(id = id * 100 + index, title = title, imageUrl = null)
        },
    )
}
