package com.artiuillab.tieryourlife.feature.community.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

@RunWith(AndroidJUnit4::class)
class AuthorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overflow_offersToHideEverythingFromThisPerson() {
        var hidden = false
        setScreen(onHideAuthor = { hidden = true })

        composeRule.onNodeWithContentDescription(string(ThemeR.string.tier_detail_content_description_more))
            .performClick()
        composeRule.onNodeWithTag(AuthorTestTags.ACTIONS_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(AuthorTestTags.ACTION_HIDE_AUTHOR).performClick()

        composeRule.runOnIdle { assertTrue(hidden) }
    }

    @Test
    fun failedState_hasNoOverflow_becauseThereIsNoOneToActOn() {
        composeRule.setContent {
            TierYourLifeTheme {
                AuthorScreenContent(
                    state = AuthorUiState.Failed,
                    onBack = {},
                    onOpenList = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag(AuthorTestTags.MORE).assertDoesNotExist()
    }

    @Test
    fun longPressOnAList_offersReporting_withoutOfferingTheProfileYouAreAlreadyOn() {
        setScreen()

        composeRule.onNodeWithTag(CommunityTestTags.communityCard("1"))
            .performTouchInput { longClick() }

        composeRule.onNodeWithTag(CommunityTestTags.LIST_ACTIONS_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_HIDE).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_VIEW_AUTHOR).assertDoesNotExist()
    }

    @Test
    fun reportingOneOfTheirLists_sendsThatListsId() {
        var reportedId: String? = null
        setScreen(onReport = { id, _, _, _ -> reportedId = id })

        composeRule.onNodeWithTag(CommunityTestTags.communityCard("1"))
            .performTouchInput { longClick() }
        composeRule.onNodeWithTag(CommunityTestTags.ACTION_REPORT).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.reportReason(ReportReason.Hate)).performClick()
        composeRule.onNodeWithTag(CommunityTestTags.REPORT_SEND).performClick()

        composeRule.runOnIdle { assertEquals("1", reportedId) }
    }

    private fun setScreen(
        onHideAuthor: () -> Unit = {},
        onReport: (String, String, ReportReason, String?) -> Unit = { _, _, _, _ -> },
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                AuthorScreenContent(
                    state = AuthorUiState.Ready(
                        name = "Olena M.",
                        photoUrl = null,
                        lists = listOf(summary("1", "Sci-fi films"), summary("2", "Every A24 film")),
                    ),
                    onBack = {},
                    onOpenList = {},
                    onRetry = {},
                    onHideAuthor = onHideAuthor,
                    onReport = onReport,
                )
            }
        }
    }

    private fun summary(id: String, title: String) = PublishedListSummary(
        id = id,
        title = title,
        authorUid = "author-1",
        authorName = "Olena M.",
        category = ListCategory.FilmTv,
        itemCount = 12,
        updatedAtMillis = 0,
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
