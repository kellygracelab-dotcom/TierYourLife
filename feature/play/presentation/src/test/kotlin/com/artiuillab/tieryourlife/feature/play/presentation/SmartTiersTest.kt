package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartTiersTest {

    private fun board(ranked: Int) = TierList(
        id = 1,
        title = "Films",
        tiers = List(ranked) { Tier(id = it + 1L, label = "T$it", colorLight = "#000000", colorDark = "#000000", items = emptyList()) } +
            Tier(id = 99, label = "Pool", colorLight = "#000000", colorDark = "#000000", items = emptyList(), isPool = true),
    )

    @Test
    fun aSmartGame_takesTheBoardsRankedTiers_notThePool() {
        assertEquals(5, smartTiers(GameKind.Smart, board(ranked = 5), keepTier = false, count = 12))
    }

    @Test
    fun oneTierBeingOrdered_givesEveryCardItsOwnPlace() {
        assertEquals(7, smartTiers(GameKind.Smart, board(ranked = 5), keepTier = true, count = 7))
    }

    @Test
    fun theTiersAreHeldBetweenOneAndTheCards() {
        assertEquals(3, smartTiers(GameKind.Smart, board(ranked = 6), keepTier = false, count = 3))
        assertEquals(1, smartTiers(GameKind.Smart, board(ranked = 0), keepTier = false, count = 4))
    }

    @Test
    fun theOtherGames_needNone() {
        assertNull(smartTiers(GameKind.Swiss, board(ranked = 5), keepTier = false, count = 12))
    }
}
