package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Rankings · N" leads the list's menu and opens the sheet; the page itself has no rankings line any more. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class RankingsInTheMenuTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val board = TierList(
        id = 0,
        title = "Every A24 film",
        tiers = listOf(
            Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(1, "Moonlight", null))),
            Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true, items = emptyList()),
        ),
    )
    private val theirs = CommunityListUiState.Success(list = board, authorName = "danylo", authorUid = "danylo", rankingCount = 13)

    private var asked = 0

    private fun show(state: CommunityListUiState.Success) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = state,
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                    onOpenRankings = { asked++ },
                )
            }
        }
    }

    @Test
    fun thePage_saysNothingAboutRankings() {
        show(theirs)

        composeRule.onNodeWithText("13 rankings").assertDoesNotExist()
        composeRule.onNodeWithText("40 rankings").assertDoesNotExist()
    }

    @Test
    fun theMenu_opensOnRankings_withTheirCount_andItOpensTheSheet() {
        show(theirs)
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithText("Rankings · 13").assertExists()
        val first = composeRule.onAllNodesWithTag(CommunityTestTags.ACTION_RANKINGS)
        first.assertCountEquals(1)
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_RANKINGS).performClick()

        composeRule.runOnIdle { assertEquals(1, asked) }
        composeRule.onNodeWithTag(CommunityListTestTags.RANKINGS_SHEET).assertExists()
    }

    @Test
    fun theirOwnList_hasRankingsInItsMenuToo() {
        show(theirs.copy(own = true))
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithText("Rankings · 13").assertExists()
    }

    @Test
    fun theirsWithNothingLeftUnranked_foldsToOneLine() {
        show(theirs)

        // Inside the pool's handle, which reads as one button.
        composeRule.onNodeWithTag(CommunityListTestTags.RANK_IT_YOURSELF, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Rank it yourself", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Drag any card", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Pool · 0 unranked", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theOneLine_isOnlyForTheirsWithAnEmptyPool_beforeTheReaderDidAnything() {
        assertTrue(theirs.foldsToRankIt)
        assertFalse(theirs.copy(showing = Showing.Mine).foldsToRankIt)
        assertFalse(theirs.copy(arranged = true).foldsToRankIt)
        assertFalse(theirs.copy(own = true).foldsToRankIt)
        val withCardsLeft = board.copy(tiers = board.tiers.map { if (it.isPool) it.copy(items = listOf(TierItem(2, "Pearl", null))) else it })
        assertFalse(theirs.copy(list = withCardsLeft, theirs = withCardsLeft).foldsToRankIt)
    }
}
