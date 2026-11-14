package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankingOfferTest {
    @Test
    fun offeredWithEveryCardPlaced_notOneShort() {
        assertNull(rankingOffer(board(20, 19), null))
        assertEquals(RankingOffer(20, 20), rankingOffer(board(20, 20), null))
        assertEquals(RankingOffer(1, 1), rankingOffer(board(1, 1), null))
        assertNull(rankingOffer(board(0, 0), null))
    }

    @Test
    fun offeredStraightAfterAGame_withSomethingPlaced() {
        assertEquals(RankingOffer(7, 20), rankingOffer(board(20, 7), null, afterGame = true))
        assertNull(rankingOffer(board(20, 0), null, afterGame = true))
    }

    @Test
    fun reopeningAndWithdrawalCannotRepeatAnOfferThatWasRecorded() {
        for (choice in RankingChoice.entries) assertNull(rankingOffer(board(20, 20), choice, afterGame = true))
    }

    private fun board(total: Int, placed: Int): TierList {
        val items = (1..total).map { TierItem(it.toLong(), "Card $it", null) }
        return TierList(
            0, "Source",
            listOf(
            Tier(0, "S", "#111111", "#EEEEEE", items.take(placed)),
            Tier(1, "Pool", "#111111", "#EEEEEE", items.drop(placed), isPool = true),
        ),
        )
    }
}
