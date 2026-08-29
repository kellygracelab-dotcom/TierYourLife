package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TIER_COUNT = 20
private const val ITEMS_PER_TIER = 75
private const val POOL_ITEMS = 500

/**
 * A board at exactly the ceiling a published list is allowed: twenty tiers and
 * two thousand cards. Items inside a tier are composed eagerly -- only the
 * tiers themselves are lazy -- so this is where that would show, in every
 * display mode and on the oldest device the app supports.
 */
@RunWith(AndroidJUnit4::class)
class TierDetailLargeListTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aBoardAtTheFullPublishedCeiling_rendersAndScrolls() {
        setScreen(TierListDisplayMode.WRAP)

        composeRule.onNodeWithTag(TierDetailTestTags.POOL_PANEL).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_LIST).performScrollToIndex(TIER_COUNT - 1)
        composeRule.onNodeWithTag(tierTag(TIER_COUNT - 1)).assertIsDisplayed()
    }

    @Test
    fun theSameBoardAsSidewaysStrips_stillScrolls() {
        setScreen(TierListDisplayMode.HORIZONTAL_SCROLL)

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_LIST).performScrollToIndex(TIER_COUNT - 1)
        composeRule.onNodeWithTag(tierTag(TIER_COUNT - 1)).assertIsDisplayed()
    }

    // The ranked view flattens every card into one column, which is the worst
    // case: no tier rows to spread the work across.
    @Test
    fun theSameBoardAsOneRankedColumn_stillScrolls() {
        setScreen(TierListDisplayMode.FLAT_RANKED)

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_LIST).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
    }

    private fun setScreen(displayMode: TierListDisplayMode) {
        val list = bigList(displayMode)
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(state = TierDetailUiState.Success(list))
            }
        }
    }

    private fun tierTag(index: Int) = TierDetailTestTags.tierRow(index.toLong())

    private fun bigList(displayMode: TierListDisplayMode): TierList {
        var nextItemId = 0L
        val tiers = (0 until TIER_COUNT).map { index ->
            Tier(
                id = index.toLong(),
                label = "T$index",
                caption = "Tier number $index",
                colorLight = "#B03A32",
                colorDark = "#F1948C",
                items = (0 until ITEMS_PER_TIER).map { TierItem(nextItemId++, "Card $nextItemId", null) },
            )
        }
        val pool = Tier(
            id = TIER_COUNT.toLong(),
            label = "Pool",
            colorLight = "#DAD7E0",
            colorDark = "#46464F",
            isPool = true,
            items = (0 until POOL_ITEMS).map { TierItem(nextItemId++, "Card $nextItemId", null) },
        )
        return TierList(
            id = 1,
            title = "A very large board",
            tiers = tiers + pool,
            displayMode = displayMode,
        )
    }
}
