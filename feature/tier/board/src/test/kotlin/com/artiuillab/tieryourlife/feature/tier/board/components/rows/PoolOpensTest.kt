package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Whether the pool under the rows starts open: where there are cards to rank, and nowhere else. */
class PoolOpensTest {

    @Test
    fun ownBoard_opensWithCardsLeft_andFoldsWhenNoneAre() {
        assertTrue(poolOpensOnItsOwn(side = null, readOnly = false, cards = 3))
        assertFalse(poolOpensOnItsOwn(side = null, readOnly = false, cards = 0))
    }

    @Test
    fun aReadOnlyBoardWithoutSides_staysFolded() {
        assertFalse(poolOpensOnItsOwn(side = null, readOnly = true, cards = 3))
    }

    @Test
    fun aSideWithCardsInItsPool_opens_evenWhenItCannotBeAddedTo() {
        assertTrue(poolOpensOnItsOwn(side = "theirs", readOnly = true, cards = 2))
        assertTrue(poolOpensOnItsOwn(side = "mine", readOnly = false, cards = 34))
    }

    @Test
    fun aSideWithAnEmptyPool_folds() {
        assertFalse(poolOpensOnItsOwn(side = "theirs", readOnly = true, cards = 0))
        assertFalse(poolOpensOnItsOwn(side = "mine", readOnly = false, cards = 0))
    }
}
