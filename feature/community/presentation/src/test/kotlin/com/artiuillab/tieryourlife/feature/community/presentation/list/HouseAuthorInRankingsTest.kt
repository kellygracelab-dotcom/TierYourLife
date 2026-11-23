package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Designer, 20.11: no "Someone" in Rankings. An author who placed nothing has no row, and the house is TierYourLife. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class HouseAuthorInRankingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val house = CommunityListUiState.Success(
        list = TierList(id = 0, title = "Pizza styles", tiers = emptyList()),
        authorName = "",
        authorUid = "",
        editorial = true,
    )
    private val olena = Ranking("olena", "Olena M.", null, emptyList(), listOf("S"), 1)

    private fun show(state: CommunityListUiState.Success) {
        composeRule.setContent {
            TierYourLifeTheme {
                RankingsSheet(state = state, onDismiss = {}, onChoose = {}, onLoadMore = {}, onWithdraw = {})
            }
        }
    }

    @Test
    fun aHouseTemplate_rankedByNobody_saysSo_andHasNoAuthorRow() {
        show(house.copy(knowsTheirs = false))

        composeRule.onNodeWithTag(CommunityListTestTags.RANKINGS_EMPTY)
            .assertTextEquals("No rankings yet. Save yours and it’ll be the first one here.")
        composeRule.onNodeWithTag(CommunityListTestTags.RANKING_AUTHOR).assertDoesNotExist()
        composeRule.onNodeWithText("Someone").assertDoesNotExist()
    }

    @Test
    fun aHouseTemplate_rankedByReaders_showsOnlyThem() {
        show(house.copy(knowsTheirs = false, rankings = listOf(olena), rankingCount = 1))

        composeRule.onNodeWithTag(CommunityListTestTags.ranking("olena")).assertIsDisplayed()
        composeRule.onNodeWithTag(CommunityListTestTags.RANKING_AUTHOR).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.RANKINGS_EMPTY).assertDoesNotExist()
    }

    @Test
    fun readersOnTheirWay_areNotNobody() {
        show(house.copy(knowsTheirs = false, rankingCount = 2, rankingsLoading = true))

        composeRule.onNodeWithTag(CommunityListTestTags.RANKINGS_EMPTY).assertDoesNotExist()
    }

    @Test
    fun aHouseListTheHousePlaced_isTierYourLifes() {
        show(house)

        composeRule.onNodeWithTag(CommunityListTestTags.RANKING_AUTHOR, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNode(hasText("TierYourLife"), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Someone").assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.RANKINGS_EMPTY).assertDoesNotExist()
    }

    @Test
    fun aPersonsTemplate_hasNoEmptyAuthorRowEither() {
        show(CommunityListUiState.Success(list = house.list, authorName = "Olena M.", authorUid = "olena", knowsTheirs = false))

        composeRule.onNodeWithTag(CommunityListTestTags.RANKING_AUTHOR).assertDoesNotExist()
        composeRule.onNodeWithTag(CommunityListTestTags.RANKINGS_EMPTY).assertIsDisplayed()
    }
}
