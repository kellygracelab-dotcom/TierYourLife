package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performFirstLinkClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Rankings · N" leads the list's menu and opens the sheet; under the title, the count links to it too (artboard 36). */
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

    private val notices = MutableSharedFlow<RankingNotice>(extraBufferCapacity = 1)

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
                    onLikeRanking = { notices.tryEmit(RankingNotice.SignInToLike) },
                    rankingNotices = notices,
                )
            }
        }
    }

    @Test
    fun underTheTitle_theRankingsCount_linksToTheSheet() {
        show(theirs)

        composeRule.onNodeWithTag(TierDetailTestTags.OWN_BAR_COUNTS)
            .assertTextEquals("1 card · 13 rankings — see how others placed them")
            .performFirstLinkClick()

        composeRule.runOnIdle { assertEquals(1, asked) }
        composeRule.onNodeWithTag(CommunityListTestTags.RANKINGS_SHEET).assertExists()
    }

    @Test
    fun withNoRankings_theLineHasNoLink() {
        show(theirs.copy(rankingCount = 0))

        composeRule.onNodeWithTag(TierDetailTestTags.OWN_BAR_COUNTS).assertTextEquals("1 card")
    }

    // Artboard 36: the check is on the placing the board shows.
    @Test
    fun theSheetsCheck_isOnWhatTheBoardShows() {
        val olena = Ranking("olena", "Olena M.", null, emptyList(), listOf("S"), 1)
        val you = Ranking("me", "Me", null, emptyList(), listOf("S"), 2, mine = true)
        var state by mutableStateOf(theirs.copy(rankings = listOf(olena, you)))
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(state = state, onBack = {}, onMoveItem = { _, _, _ -> }, onSave = {}, onRetry = {})
            }
        }
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_RANKINGS).performClick()
        val author = composeRule.onNodeWithTag(CommunityListTestTags.RANKING_AUTHOR)
        val theirsRow = composeRule.onNodeWithTag(CommunityListTestTags.ranking("olena"))
        val yours = composeRule.onNodeWithTag(CommunityListTestTags.ranking("me"))

        author.assertIsSelected()
        theirsRow.assertIsNotSelected()
        yours.assertIsNotSelected()

        state = state.copy(chosenRanking = olena)
        author.assertIsNotSelected()
        theirsRow.assertIsSelected()

        state = state.copy(showing = Showing.Mine)
        author.assertIsNotSelected()
        theirsRow.assertIsNotSelected()
        yours.assertIsSelected()

        state = state.copy(rankings = listOf(olena))
        author.assertIsNotSelected()
        theirsRow.assertIsNotSelected()
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

    // QA list-flow Н7: a guest's Like is answered on the sheet, not under it.
    @Test
    fun aGuestsLike_asksToSignIn_onTheSheet() {
        show(theirs.copy(rankings = listOf(Ranking("olena", "Olena M.", null, emptyList(), listOf("S"), 1))))
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_RANKINGS).performClick()

        composeRule.onNodeWithTag(CommunityListTestTags.like("olena")).performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Sign in to like rankings") and hasAnyAncestor(hasTestTag(CommunityListTestTags.RANKINGS_SHEET)))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun theirOwnList_hasRankingsInItsMenuToo() {
        show(theirs.copy(own = true))
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithText("Rankings · 13").assertExists()
    }

    @Test
    fun anUnrankedHouseList_opensRankingsFromMine_withoutOfferingToEditTheSource() {
        show(theirs.copy(editorial = true, showing = Showing.Mine, rankingCount = 0))

        composeRule.onNodeWithTag(TierDetailTestTags.MANUAL_ADD_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Edit list name").assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_RANKINGS).performClick()

        composeRule.runOnIdle { assertEquals(1, asked) }
        composeRule.onNodeWithTag(CommunityListTestTags.RANKINGS_SHEET).assertExists()
    }

    @Test
    fun theirsWithNothingLeftUnranked_foldsToOneLine() {
        show(theirs)

        // Inside the pool's handle, which reads as one button.
        composeRule.onNodeWithTag(CommunityListTestTags.RANK_IT_YOURSELF, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Rank it yourself", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Drag any card", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("0 cards left", useUnmergedTree = true).assertDoesNotExist()
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
