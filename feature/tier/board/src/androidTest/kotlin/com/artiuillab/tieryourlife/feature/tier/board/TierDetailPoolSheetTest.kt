package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The pool is a sheet: open where there is ranking to do, folded where there is not, and a tap switches. */
@RunWith(AndroidJUnit4::class)
class TierDetailPoolSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anOwnBoardWithCardsLeftToRank_opensWithThePoolOut() {
        setScreen(board(unranked = 2), readOnly = false)

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertIsDisplayed()

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_TOGGLE).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.ADD_CHIP).assertIsDisplayed()
    }

    @Test
    fun aReadersPool_startsFolded_andOpensOnATap() {
        setScreen(board(unranked = 2), readOnly = true)

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_PANEL).assertIsDisplayed()

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_TOGGLE).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertIsDisplayed()
    }

    @Test
    fun anEmptyPool_startsFolded_withTheAddChipStillInReach() {
        setScreen(board(unranked = 0), readOnly = false)

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_ITEMS).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.ADD_CHIP).assertIsDisplayed()
    }

    private fun setScreen(list: TierList, readOnly: Boolean) {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(list),
                    actions = TierDetailActions(),
                    readOnly = readOnly,
                )
            }
        }
    }

    private fun board(unranked: Int) = TierList(
        id = 1,
        title = "Sci-fi films",
        tiers = listOf(
            Tier(id = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(1, "Arrival", null))),
            Tier(
                id = -1,
                label = "Unranked",
                colorLight = "#DAD7E0",
                colorDark = "#46464F",
                items = List(unranked) { TierItem(100L + it, "Card $it", null) },
                isPool = true,
            ),
        ),
    )
}
