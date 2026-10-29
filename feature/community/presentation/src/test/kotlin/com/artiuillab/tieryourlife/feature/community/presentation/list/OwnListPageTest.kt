package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityTestTags
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedCallbacks
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedListMenus
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** One's own list, named or anonymous, reads as others see it: nothing to save, hide, report or follow. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class OwnListPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val board = TierList(
        id = 0,
        title = "Every A24 film",
        tiers = listOf(
            Tier(id = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(1, "Moonlight", null))),
            Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true, items = emptyList()),
        ),
    )

    private val theirs = CommunityListUiState.Success(list = board, authorName = "Olena M.", authorUid = "olena")
    private val mineNamed = theirs.copy(authorName = "Danylo", authorUid = "me", own = true, ownBoardId = 12)
    private val mineAnonymous = theirs.copy(authorName = "", authorUid = "", editorial = true, byNobody = true, own = true)

    private fun show(state: CommunityListUiState.Success, onOpenSaved: (Long) -> Unit = {}) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(state = state, onBack = {}, onMoveItem = { _, _, _ -> }, onSave = {}, onRetry = {}, onOpenSaved = onOpenSaved)
            }
        }
    }

    @Test
    fun myOwnList_hasNoSaveAndNoSwitch_butOpensMyBoard() {
        val opened = mutableListOf<Long>()
        show(mineNamed, onOpenSaved = { opened += it })

        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.showing(Showing.Mine)).assertDoesNotExist()
        composeRule.onNodeWithText("Your list, as others see it.").assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.OPEN_BOARD).performClick()

        composeRule.runOnIdle { assertEquals(listOf(12L), opened) }
    }

    @Test
    fun myOwnList_sMenu_offersNeitherHideNorReport() {
        show(mineNamed)

        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithTag(CommunityTestTags.LIST_ACTIONS_SHEET).assertExists()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_HIDE).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).assertDoesNotExist()
    }

    @Test
    fun myOwnAnonymousList_hasNoMenuAtAll_andNoBoardToOpenWhenThisPhoneHasNone() {
        show(mineAnonymous)

        composeRule.onNodeWithTag(CommunityListTestTags.MORE).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.OPEN_BOARD).assertDoesNotExist()
    }

    @Test
    fun somebodyElsesList_stillOffersSave_andItsMenuHideAndReport() {
        show(theirs)

        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_HIDE).assertExists()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).assertExists()
    }

    private fun card(authorUid: String, mine: Boolean, anonymous: Boolean = false) = PublishedListSummary(
        id = "l",
        title = "Every A24 film",
        authorUid = authorUid,
        authorName = if (anonymous) "" else "Danylo",
        category = ListCategory.FilmTv.id,
        itemCount = 1,
        updatedAtMillis = 0,
        anonymous = anonymous,
        mine = mine,
    )

    private fun pressAndHold(summary: PublishedListSummary) {
        composeRule.setContent {
            TierYourLifeTheme { FeedListMenus(target = summary, onDismiss = {}, feed = FeedCallbacks(), onOpenAuthor = { _, _, _ -> }) }
        }
    }

    @Test
    fun aFeedCardOfMine_isNotOfferedToHideOrReport() {
        pressAndHold(card(authorUid = "me", mine = true))

        composeRule.onNodeWithTag(CommunityTestTags.ACTION_VIEW_AUTHOR).assertExists()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_HIDE).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).assertDoesNotExist()
    }

    @Test
    fun aFeedCardOfMineWithoutMyName_opensNoEmptyMenu() {
        pressAndHold(card(authorUid = "", mine = true, anonymous = true))

        composeRule.onNodeWithTag(CommunityTestTags.LIST_ACTIONS_SHEET).assertDoesNotExist()
    }

    @Test
    fun somebodyElsesFeedCard_canBeHiddenAndReported() {
        pressAndHold(card(authorUid = "olena", mine = false))

        composeRule.onNodeWithTag(CommunityTestTags.ACTION_HIDE).assertExists()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).assertExists()
    }
}
