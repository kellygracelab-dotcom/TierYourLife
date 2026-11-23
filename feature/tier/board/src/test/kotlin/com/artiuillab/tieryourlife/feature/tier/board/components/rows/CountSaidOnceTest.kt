package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Designer, 23.11: the count is said once, under the title; the ranked list only says which way it reads. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class CountSaidOnceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun board(placed: Int, left: Int): TierList {
        var id = 0L
        fun cards(n: Int) = List(n) { TierItem(++id, "Card $id", null) }
        return TierList(
            id = 1,
            title = "Games",
            tiers = listOf(
                Tier(1, "S", "#B03A32", "#F1948C", cards(placed)),
                Tier(99, "Pool", "#DAD7E0", "#46464F", cards(left), isPool = true),
            ),
        )
    }

    private fun countsOf(list: TierList): String {
        var counts = ""
        composeRule.setContent { counts = boardCounts(list) }
        composeRule.waitForIdle()
        return counts
    }

    @Test
    fun everyCardPlaced_justTheCards() {
        assertEquals("159 cards", countsOf(board(placed = 159, left = 0)))
    }

    @Test
    fun somePlaced_theCardsAndHowManyArePlaced() {
        assertEquals("159 cards · 120 placed", countsOf(board(placed = 120, left = 39)))
    }

    @Test
    fun nonePlaced_saysSo() {
        assertEquals("3 cards · nothing placed yet", countsOf(board(placed = 0, left = 3)))
    }

    @Test
    fun theRankedList_saysOnlyWhichWayItReads() {
        val list = board(placed = 5, left = 0)
        composeRule.setContent { TierYourLifeTheme { RankedList(list.tiers.filterNot { it.isPool }, onSelect = {}) } }

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_HEADER).assertTextEquals("Best to worst")
    }

    @Test
    fun anEmptyRankedList_saysNothingIsPlaced() {
        composeRule.setContent { TierYourLifeTheme { RankedList(emptyList(), onSelect = {}) } }

        composeRule.onNodeWithTag(TierDetailTestTags.RANKED_HEADER).assertTextEquals("Nothing placed yet")
    }
}
