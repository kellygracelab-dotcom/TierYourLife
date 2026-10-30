package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

@RunWith(AndroidJUnit4::class)
class CommunityListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readerOverflow_offersTheirProfileHidingAndReporting() {
        setScreen()

        openOverflow()

        composeRule.onNodeWithTag(CommunityTestTags.LIST_ACTIONS_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_VIEW_AUTHOR).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_HIDE).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).assertIsDisplayed()
    }

    @Test
    fun hiding_leavesTheListRatherThanSittingOnAScreenYouJustHid() {
        var hidden = false
        setScreen(onHide = { hidden = true })

        openOverflow()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_HIDE).performClick()

        composeRule.runOnIdle { assertTrue(hidden) }
    }

    @Test
    fun reporting_asksForAReasonBeforeItWillSend() {
        setScreen()

        openOverflow()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).performClick()

        composeRule.onNodeWithTag(CommunityTestTags.REPORT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityTestTags.REPORT_SEND).assertIsNotEnabled()
    }

    @Test
    fun reporting_sendsTheReason_andSaysWhatHappensNext() {
        var reported: ReportReason? = null
        setScreen(onReport = { reason, _ -> reported = reason })

        openOverflow()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.reportReason(ReportReason.Spam)).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.REPORT_SEND).performClick()

        composeRule.runOnIdle { assertEquals(ReportReason.Spam, reported) }
        composeRule.onNodeWithText(string(R.string.report_sent_title)).assertIsDisplayed()
    }

    @Test
    fun editorialList_showsNoAuthorLine_andItsOverflowLeadsToNoProfile() {
        setScreen(state = successState().copy(editorial = true))

        composeRule.onNodeWithText(string(R.string.community_by_author, "Olena M.")).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.FOLLOW).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.community_rank_it_your_way)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.community_someone_elses)).assertDoesNotExist()

        openOverflow()
        composeRule.onNodeWithTag(CommunityTestTags.LIST_ACTIONS_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_VIEW_AUTHOR).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_HIDE).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).assertIsDisplayed()
    }

    @Test
    fun reportingAnEditorialList_offersNoWayToHideItsAuthor() {
        setScreen(state = successState().copy(editorial = true))

        openOverflow()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.reportReason(ReportReason.Spam)).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.REPORT_SEND).performClick()

        composeRule.onNodeWithText(string(R.string.report_sent_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.report_hide_author, "Olena M.")).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.action_done)).assertIsDisplayed()
    }

    @Test
    fun loadingState_keepsTheOverflowAwayUntilThereIsSomethingToActOn() {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = CommunityListUiState.Loading,
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag(CommunityListTestTags.MORE).assertDoesNotExist()
    }

    @Test
    fun whileLoading_whatTheCardShowed_isAlreadyOnScreen() {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = CommunityListUiState.Loading,
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                    preview = ListPreview(title = "Sci-fi films", authorName = "Olena M.", itemCount = 6, tierColors = listOf("#B03A32")),
                )
            }
        }

        composeRule.onNodeWithTag(CommunityListTestTags.SKELETON).assertIsDisplayed()
        composeRule.onNodeWithText("Sci-fi films").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.community_by_author, "Olena M.")).assertIsDisplayed()
    }

    @Test
    fun onceSaved_theBarOffersTheCopyInsteadOfSavingAgain() {
        var opened: Long? = null
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = successState().copy(savedId = 7L),
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                    onOpenSaved = { opened = it },
                )
            }
        }

        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.OPEN_SAVED).performClick()
        assertEquals(7L, opened)
    }

    // The author's arrangement usually leaves nothing unranked, so the pool
    // starts folded on it; what the reader does with the board must not fold away with it.
    @Test
    fun theSwitchAndTheSave_sitInTheFoldedStrip() {
        setScreen(state = successState().withEveryCardRanked())

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.showing(Showing.Theirs)).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertIsDisplayed()
    }

    // Cards left in the pool of the side on screen open it, and the strip stays above them.
    @Test
    fun aPoolWithCardsInIt_opens_withTheSwitchAndTheSaveStillThere() {
        setScreen()

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityListTestTags.showing(Showing.Theirs)).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertIsDisplayed()
    }

    // A reader who glanced at the author's half and tapped Save did not mean
    // to throw their own away, nor the other way round: the save asks.
    @Test
    fun saving_asksWhoseArrangementToKeep() {
        var chosen: Showing? = null
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = successState(),
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = { chosen = it },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).performClick()
        composeRule.onNodeWithTag(CommunityListTestTags.SAVE_CHOICE).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityListTestTags.SAVE_THEIRS).performClick()

        assertEquals(Showing.Theirs, chosen)
    }

    private fun openOverflow() {
        composeRule.onNodeWithContentDescription(string(ThemeR.string.tier_detail_content_description_more))
            .performClick()
    }

    private fun setScreen(
        state: CommunityListUiState = successState(),
        onHide: () -> Unit = {},
        onReport: (ReportReason, String?) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = state,
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                    onHide = onHide,
                    onReport = onReport,
                )
            }
        }
    }

    private fun successState() = CommunityListUiState.Success(
        list = TierList(
            id = 0,
            title = "Sci-fi films",
            tiers = listOf(
                Tier(id = 1, label = "S", colorLight = "#F2B8B5", colorDark = "#8C1D18", items = emptyList()),
                Tier(
                    id = -1,
                    label = "Unranked",
                    colorLight = "#DAD7E0",
                    colorDark = "#46464F",
                    isPool = true,
                    items = listOf(TierItem(1, "Arrival", null)),
                ),
            ),
            authorName = "Olena M.",
        ),
        authorName = "Olena M.",
        authorUid = "author-1",
    )

    /** Every card in a tier, as an author's arrangement usually leaves them: nothing in the pool. */
    private fun CommunityListUiState.Success.withEveryCardRanked(): CommunityListUiState.Success {
        val cards = list.tiers.flatMap { it.items }
        val ranked = list.copy(tiers = list.tiers.map { tier -> tier.copy(items = if (tier.isPool) emptyList() else cards) })
        return copy(list = ranked, theirs = ranked)
    }

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
}
