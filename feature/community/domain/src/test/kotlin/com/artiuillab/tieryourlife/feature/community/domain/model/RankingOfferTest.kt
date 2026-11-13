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
    fun thresholdIsStrict_andDoesNotAddAFiveCardMinimum() {
        assertNull(rankingOffer(board(20, 6), null))
        assertEquals(RankingOffer(7, 20), rankingOffer(board(20, 7), null))
        assertNull(rankingOffer(board(127, 38), null))
        assertEquals(RankingOffer(39, 127), rankingOffer(board(127, 39), null))
        assertEquals(RankingOffer(1, 3), rankingOffer(board(3, 1), null))
    }

    @Test
    fun reopeningAndWithdrawalCannotRepeatAnOfferThatWasRecorded() {
        for (choice in RankingChoice.entries) assertNull(rankingOffer(board(20, 15), choice))
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
