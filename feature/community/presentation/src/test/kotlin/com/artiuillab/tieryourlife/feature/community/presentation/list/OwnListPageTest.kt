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

/** One's own list, named or anonymous, reads as others see it, says it is theirs, and always has something to do. */
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

    private fun show(state: CommunityListUiState.Success, onOpenSaved: (Long) -> Unit = {}, own: OwnListCallbacks = OwnListCallbacks()) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = state,
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                    onOpenSaved = onOpenSaved,
                    own = own,
                )
            }
        }
    }

    @Test
    fun myOwnList_saysItIsMine_hasNoSaveAndNoSwitch_butOpensMyBoardAndCopiesTheLink() {
        val opened = mutableListOf<Long>()
        show(mineNamed, onOpenSaved = { opened += it })

        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.showing(Showing.Mine)).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.YOUR_LIST).assertExists()
        composeRule.onNodeWithText("Published").assertExists()
        composeRule.onNodeWithText("This is what visitors see. Changes happen on your board.").assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.COPY_LINK).assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.OPEN_BOARD).performClick()

        composeRule.runOnIdle { assertEquals(listOf(12L), opened) }
    }

    @Test
    fun myOwnList_sMenu_offersMyPublicLists_myName_andTakingItDown_butNeitherHideNorReport() {
        val takenDown = mutableListOf<Unit>()
        val anonymously = mutableListOf<Boolean>()
        show(mineNamed, own = OwnListCallbacks(onTakeDown = { takenDown += Unit }, onPublishAnonymously = { anonymously += it }))

        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithTag(CommunityTestTags.LIST_ACTIONS_SHEET).assertExists()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_HIDE).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_YOUR_PUBLIC_LISTS).assertExists()
        composeRule.onNodeWithText("Publish without my name").performClick()
        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_TAKE_DOWN).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(true), anonymously)
            assertEquals(1, takenDown.size)
        }
    }

    // Danylo opened his own anonymous list and nothing said it was his, nor was there anything to do.
    @Test
    fun myOwnAnonymousList_withNoBoardHere_saysItIsMine_andOffersToGetItBack() {
        val gotBack = mutableListOf<Unit>()
        show(mineAnonymous.copy(ownBoardLookedUp = true), own = OwnListCallbacks(onGetBack = { gotBack += Unit }))

        composeRule.onNodeWithTag(CommunityListTestTags.YOUR_LIST).assertExists()
        composeRule.onNodeWithText("Anonymous · only you see it's yours").assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.OPEN_BOARD).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.GET_BACK_BOARD).performClick()

        composeRule.runOnIdle { assertEquals(1, gotBack.size) }
    }

    @Test
    fun myOwnAnonymousList_hasAMenu_withTakeDown_butNoNameToChangeWithoutTheBoard() {
        show(mineAnonymous.copy(ownBoardLookedUp = true))

        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()

        composeRule.onNodeWithTag(CommunityTestTags.ACTION_TAKE_DOWN).assertExists()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_YOUR_PUBLIC_LISTS).assertExists()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_PUBLISH_ANONYMOUSLY).assertDoesNotExist()
    }

    @Test
    fun myOwnAnonymousList_withItsBoardHere_offersToPutMyNameBack() {
        val anonymously = mutableListOf<Boolean>()
        show(mineAnonymous.copy(anonymous = true, ownBoardId = 12, ownBoardLookedUp = true), own = OwnListCallbacks(onPublishAnonymously = { anonymously += it }))

        composeRule.onNodeWithTag(CommunityListTestTags.MORE).performClick()
        composeRule.onNodeWithText("Publish with my name").performClick()

        composeRule.runOnIdle { assertEquals(listOf(false), anonymously) }
    }

    @Test
    fun myOwnList_withItsBoardInTheTrash_offersToRestoreIt() {
        val restored = mutableListOf<Unit>()
        show(mineNamed.copy(ownBoardId = null, trashedBoardId = 5, ownBoardLookedUp = true), own = OwnListCallbacks(onRestore = { restored += Unit }))

        composeRule.onNodeWithTag(CommunityListTestTags.OPEN_BOARD).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.RESTORE_BOARD).performClick()

        composeRule.runOnIdle { assertEquals(1, restored.size) }
    }

    @Test
    fun myOwnList_beforeThisPhoneHasAnswered_offersNoBoardButtonYet() {
        show(mineNamed.copy(ownBoardId = null))

        composeRule.onNodeWithTag(CommunityListTestTags.OPEN_BOARD).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.GET_BACK_BOARD).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.COPY_LINK).assertExists()
    }

    // A phone on its side: one short bar carries all of it.
    @Test
    @Config(qualifiers = "en-w891dp-h411dp")
    fun myOwnList_inAShortWindowsBar_saysItIsMine_andKeepsTheButtons() {
        show(mineNamed.copy(rankingCount = 3))

        composeRule.onNodeWithTag(CommunityListTestTags.YOUR_LIST).assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.BAR_AUTHOR).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.COPY_LINK).assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.OPEN_BOARD).assertExists()
    }

    // A tablet held upright: the chip and the meta line take the author line's place under the title.
    @Test
    @Config(qualifiers = "en-w800dp-h1280dp")
    fun myOwnList_onAnUprightTablet_saysItIsMineUnderTheTitle() {
        show(mineNamed.copy(rankingCount = 3))

        composeRule.onNodeWithTag(CommunityListTestTags.YOUR_LIST).assertExists()
        composeRule.onNodeWithText("Published").assertExists()
        composeRule.onNodeWithTag(CommunityListTestTags.OPEN_BOARD).assertExists()
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
