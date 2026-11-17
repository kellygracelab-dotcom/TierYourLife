package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Test

/** The Play window warns about cards already in tiers, as the site does (artboards 15/24). */
class PlacedChosenTest {

    private val board = TierList(
        id = 1,
        title = "Films",
        tiers = listOf(
            tier(1, ids = 1L..3L),
            tier(2, ids = 4L..5L),
            tier(99, ids = 6L..10L, pool = true),
        ),
    )

    @Test
    fun theWholeBoard_countsTheCardsInTiers_notThePool() {
        assertEquals(5, picker(chosen = setOf(1, 2, 99)).placedChosen)
        assertEquals(5, picker(chosen = setOf(1, 2, 99), withCards = false).placedChosen)
    }

    @Test
    fun thePoolAlone_hasNothingPlaced() {
        assertEquals(0, picker(chosen = setOf(99)).placedChosen)
    }

    @Test
    fun oneTierBeingOrdered_staysInItsTier_soThereIsNoWarning() {
        assertEquals(0, picker(chosen = setOf(1)).placedChosen)
    }

    @Test
    fun aHandPickedSelection_countsOnlyItsPlacedCards() {
        assertEquals(2, picker(chosen = setOf(1, 2, 99), selected = listOf(1L, 4L, 7L, 8L)).placedChosen)
    }

    private fun picker(chosen: Set<Long>, selected: List<Long>? = null, withCards: Boolean = true) =
        Picker(board.title, groupsOf(board), chosen, cards = if (withCards) cardsOf(board) else emptyList(), selectedIds = selected)

    private fun tier(id: Long, ids: LongRange, pool: Boolean = false) =
        Tier(id = id, label = "T$id", colorLight = "#000000", colorDark = "#000000", items = ids.map { TierItem(it, "Card $it", null) }, isPool = pool)
}
