package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A starred board lives on this phone as much as any other, so the card and the footer count it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class LocalOnlyCountsStarredTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun twoStarredAndOneNot_areThreeBoards() {
        composeRule.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(
                    state = TierListsUiState.Success(
                        lists = listOf(board(3)),
                        favourites = listOf(board(1, starred = true), board(2, starred = true)),
                        grouped = true,
                        totalListCount = 3,
                        rankedCount = 0,
                        localOnly = LocalOnly.Here(offerSignIn = true),
                    ),
                    onTierListClick = {},
                )
            }
        }

        composeRule.onNodeWithText("These boards live on this phone").assertExists()
        composeRule.onNodeWithTag(TierListsTestTags.LISTS).performScrollToNode(hasTestTag(TierListsTestTags.LOCAL_ONLY_FOOTER))
        composeRule.onNodeWithTag(TierListsTestTags.LOCAL_ONLY_FOOTER).assertTextEquals("3 boards, on this phone only")
    }

    private fun board(id: Long, starred: Boolean = false) = TierListSummary(
        id = id,
        title = "Board $id",
        tiers = emptyList(),
        editedAt = id,
        favouritedAt = if (starred) id else null,
    )
}
