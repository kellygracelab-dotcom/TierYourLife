package com.artiuillab.tieryourlife.feature.tier.presentation.community

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
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommunityListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readerOverflow_offersTheirProfileHidingAndReporting() {
        setScreen()

        openOverflow()

        composeRule.onNodeWithTag(TierListsTestTags.LIST_ACTIONS_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(TierListsTestTags.ACTION_VIEW_AUTHOR).assertIsDisplayed()
        composeRule.onNodeWithTag(TierListsTestTags.ACTION_HIDE).assertIsDisplayed()
        composeRule.onNodeWithTag(TierListsTestTags.ACTION_REPORT).assertIsDisplayed()
    }

    @Test
    fun hiding_leavesTheListRatherThanSittingOnAScreenYouJustHid() {
        var hidden = false
        setScreen(onHide = { hidden = true })

        openOverflow()
        composeRule.onNodeWithTag(TierListsTestTags.ACTION_HIDE).performClick()

        composeRule.runOnIdle { assertTrue(hidden) }
    }

    @Test
    fun reporting_asksForAReasonBeforeItWillSend() {
        setScreen()

        openOverflow()
        composeRule.onNodeWithTag(TierListsTestTags.ACTION_REPORT).performClick()

        composeRule.onNodeWithTag(TierListsTestTags.REPORT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(TierListsTestTags.REPORT_SEND).assertIsNotEnabled()
    }

    @Test
    fun reporting_sendsTheReason_andSaysWhatHappensNext() {
        var reported: ReportReason? = null
        setScreen(onReport = { reason, _ -> reported = reason })

        openOverflow()
        composeRule.onNodeWithTag(TierListsTestTags.ACTION_REPORT).performClick()
        composeRule.onNodeWithTag(TierListsTestTags.reportReason(ReportReason.Spam)).performClick()
        composeRule.onNodeWithTag(TierListsTestTags.REPORT_SEND).performClick()

        composeRule.runOnIdle { assertEquals(ReportReason.Spam, reported) }
        composeRule.onNodeWithText(string(R.string.report_sent_title)).assertIsDisplayed()
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

        composeRule.onNodeWithTag(CommunityTestTags.MORE).assertDoesNotExist()
    }

    private fun openOverflow() {
        composeRule.onNodeWithContentDescription(string(R.string.tier_detail_content_description_more))
            .performClick()
    }

    private fun setScreen(
        onHide: () -> Unit = {},
        onReport: (ReportReason, String?) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = successState(),
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
            authorName = "Danylo K.",
        ),
        authorName = "Danylo K.",
        authorUid = "author-1",
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
