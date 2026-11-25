package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Danylo 25.11, on Android and the web alike: no Author | Mine switch. The reader's
 * version with a link to theirs, theirs only to look at with the way back, and
 * both menu actions done at once with Undo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class WhoseVersionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val board = TierList(
        id = 0,
        title = "Every A24 film",
        tiers = listOf(
            Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(1, "Moonlight", null))),
            Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true, items = listOf(TierItem(2, "Lady Bird", null))),
        ),
    )
    private val emptied = board.copy(tiers = board.tiers.map { if (it.isPool) it.copy(items = board.tiers.flatMap { tier -> tier.items }) else it.copy(items = emptyList()) })
    private val theirs = CommunityListUiState.Success(list = board, mine = emptied, authorName = "Olena M.", authorUid = "olena")
    private val yours = theirs.copy(mine = board, arranged = true, showing = Showing.Mine)

    private val shown = mutableListOf<Showing>()
    private var saves = 0
    private var scratch = 0
    private var backToTheirs = 0
    private val undone = mutableListOf<Long>()

    private fun show(state: CommunityListUiState.Success, notices: Flow<ListNotice> = emptyFlow()) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = state,
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = { saves++ },
                    onRetry = {},
                    onShow = { shown += it },
                    onStartFromScratch = { scratch++ },
                    onBackToTheirs = { backToTheirs++ },
                    onUndoStartFromTheirs = { undone += it },
                    listNotices = notices,
                )
            }
        }
    }

    @Test
    fun beforeAVersion_thereIsNoSwitchAndNoLink() {
        show(theirs)

        composeRule.onNodeWithTag(CommunityListTestTags.SEE_THEIRS).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.BACK_TO_YOURS).assertDoesNotExist()
        composeRule.onNodeWithText("Author").assertDoesNotExist()
    }

    @Test
    fun yourVersion_linksToTheirs() {
        show(yours)

        composeRule.onNodeWithText("Your version · ").assertExists()
        composeRule.onNodeWithText("see Olena M.’s").performClick()

        composeRule.runOnIdle { assertEquals(listOf(Showing.Theirs), shown) }
    }

    // Designer 25.11, as on the web: Save author's list before a version, Save my list after.
    @Test
    fun beforeAVersion_saveKeepsTheAuthors() {
        show(theirs)

        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertExists()
        composeRule.onNodeWithText("Save author’s list").assertExists()
    }

    @Test
    fun yourVersion_saveKeepsYours() {
        show(yours)

        composeRule.onNodeWithText("Save my list").assertExists()
    }

    // Looking at theirs, the same Save my list keeps the reader's version, not theirs.
    @Test
    fun theirsOverYours_isNamed_savesYours_andLeadsBack() {
        show(yours.copy(showing = Showing.Theirs))

        composeRule.onNodeWithTag(CommunityListTestTags.THEIRS_RANKING, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Olena M.’s ranking", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Save author’s list").assertDoesNotExist()
        composeRule.onNodeWithText("Save my list").performClick()
        composeRule.runOnIdle { assertEquals(1, saves) }
        composeRule.onNodeWithTag(CommunityListTestTags.SEE_THEIRS).assertDoesNotExist()
        composeRule.onNodeWithText("Back to yours").performClick()

        composeRule.runOnIdle { assertEquals(listOf(Showing.Mine), shown) }
    }

    @Test
    fun anAuthorWithoutAName_isTheAuthor() {
        show(yours.copy(showing = Showing.Theirs, authorName = "", editorial = true))

        composeRule.onNodeWithText("The author’s ranking", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theMenu_backToTheirs_actsAtOnce() {
        show(yours)
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithText("Back to Olena M.’s ranking").performClick()

        composeRule.runOnIdle { assertEquals(1, backToTheirs) }
        composeRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun theMenu_startFromScratch_actsAtOnce() {
        show(yours)
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithTag(CommunityTestTags.ACTION_START_FROM_SCRATCH).performClick()

        composeRule.runOnIdle { assertEquals(1, scratch) }
        composeRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun theMenu_beforeAVersion_offersNeither() {
        show(theirs)
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithTag(CommunityTestTags.ACTION_BACK_TO_THEIRS).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_START_FROM_SCRATCH).assertDoesNotExist()
    }

    @Test
    fun aFreshStart_isSaid_withUndo() {
        show(yours, notices = flowOf(ListNotice.StartedFromScratch(undoId = 4)))

        composeRule.onNodeWithText("All cards are back in the deck").assertExists()
        composeRule.onNodeWithText("Undo").performClick()

        composeRule.runOnIdle { assertEquals(listOf(4L), undone) }
    }
}
