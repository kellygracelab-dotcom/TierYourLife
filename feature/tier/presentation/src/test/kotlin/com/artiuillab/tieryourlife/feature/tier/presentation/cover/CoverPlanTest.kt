package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The arithmetic and the rules behind a list on the cover (§5): what fits a row, which row is open, which list is next. */
class CoverPlanTest {

    @Test
    fun aCoverRow_holdsSevenPlaces() {
        // 352dp less 12dp each side: a 32dp band, 5dp padding, 36dp cards 4dp apart.
        assertEquals(7, coverCardsPerLine(328))
    }

    @Test
    fun aNarrowRow_stillHoldsOneCard() {
        assertEquals(1, coverCardsPerLine(40))
    }

    @Test
    fun cardsThatFit_areAllShown_withNoMoreTile() {
        assertEquals(CoverRowPlan(shown = 7, more = 0), coverRowPlan(cards = 7, perLine = 7, open = false))
        assertEquals(CoverRowPlan(shown = 3, more = 0), coverRowPlan(cards = 3, perLine = 7, open = false))
    }

    @Test
    fun oneCardTooMany_givesTheLastPlaceToPlusN() {
        assertEquals(CoverRowPlan(shown = 6, more = 2), coverRowPlan(cards = 8, perLine = 7, open = false))
    }

    @Test
    fun aFullRow_saysHowManyAreNotShown() {
        assertEquals(CoverRowPlan(shown = 6, more = 34), coverRowPlan(cards = 40, perLine = 7, open = false))
    }

    @Test
    fun anOpenRow_showsEveryCard() {
        assertEquals(CoverRowPlan(shown = 40, more = 0), coverRowPlan(cards = 40, perLine = 7, open = true))
    }

    @Test
    fun tappingARow_opensIt() {
        assertEquals(3L, toggledRow(open = null, tapped = 3L))
    }

    @Test
    fun tappingTheOpenRow_foldsIt() {
        assertNull(toggledRow(open = 3L, tapped = 3L))
    }

    @Test
    fun tappingAnotherRow_movesTheOpeningThere_soOnlyOneIsOpen() {
        assertEquals(5L, toggledRow(open = 3L, tapped = 5L))
    }

    private val films = TierList(1, "Films", emptyList())
    private val games = TierList(2, "Games", emptyList())
    private val coffee = TierList(3, "Coffee", emptyList())

    @Test
    fun theNextList_isTheOneBelowInTheCoversOrder() {
        assertEquals(games, nextListAfter(listOf(films, games, coffee), currentId = 1))
    }

    @Test
    fun theLastList_hasNoNext() {
        assertNull(nextListAfter(listOf(films, games, coffee), currentId = 3))
    }

    @Test
    fun aListThatIsGone_hasNoNext() {
        assertNull(nextListAfter(listOf(films, games, coffee), currentId = 99))
    }

    @Test
    fun theCoversOrder_putsStarredListsFirst_once() {
        val state = TierListsUiState.Success(
            lists = listOf(films, games, coffee),
            totalListCount = 3,
            rankedCount = 0,
            favourites = listOf(coffee),
        )
        assertEquals(listOf(coffee, films, games), coverOrder(state))
    }

    @Test
    fun beforeTheListsAreRead_thereAreNone() {
        assertEquals(emptyList<TierList>(), coverOrder(TierListsUiState.Loading))
    }
}
