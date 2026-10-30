package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Test

/** A reader's board and a ranking are two ways of saying the same placing. */
class RankingArrangementTest {

    private fun tier(id: Long, label: String) = Tier(id = id, label = label, colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList())

    private val answer = PublishedList(
        summary = PublishedListSummary(
            id = "abc",
            title = "Films",
            authorUid = "olena",
            authorName = "Olena",
            category = ListCategory.FilmTv.id,
            itemCount = 4,
            updatedAtMillis = 0,
        ),
        tiers = listOf(tier(0, "S"), tier(1, "A"), tier(2, "B")),
        items = (0L..3L).map { TierItem(it, "Film $it", null) },
    )

    private val ranking = Ranking(
        uid = "me",
        authorName = "Ann",
        authorPhotoUrl = null,
        arrangement = listOf(2, null, 0, 0),
        tierLabels = listOf("Love", "Like", "Meh"),
        updatedAtMillis = 0,
        mine = true,
    )

    @Test
    fun aRankingPlacedOnABoard_readsBackAsTheSameRanking() {
        val board = placedBy(answer, ranking)

        assertEquals(ranking.arrangement, arrangementOf(answer, board))
        assertEquals(ranking.tierLabels, tierLabelsOf(answer, board))
    }

    @Test
    fun cardsAreMatchedById_whereverTheyStandInTheirRow() {
        val board = TierList(
            id = 0,
            title = "Films",
            tiers = listOf(
                tier(0, "S").copy(items = listOf(TierItem(3, "Film 3", null), TierItem(1, "Film 1", null))),
                tier(1, "A"),
                tier(2, "B").copy(items = listOf(TierItem(0, "Film 0", null))),
                pool(listOf(TierItem(2, "Film 2", null))),
            ),
        )

        assertEquals(listOf(2, 0, null, 0), arrangementOf(answer, board))
        assertEquals(listOf("S", "A", "B"), tierLabelsOf(answer, board))
    }

    @Test
    fun aBoardWithNothingPlaced_sendsEveryCardAsUnplaced() {
        val untouched = TierList(id = 0, title = "Films", tiers = answer.tiers + pool(answer.items))

        assertEquals(listOf(null, null, null, null), arrangementOf(answer, untouched))
        assertEquals(listOf(emptyList<Int>(), emptyList(), emptyList()), rowsOf(answer, untouched))
    }

    @Test
    fun rows_sayEachTiersCardsLeftToRight_byTheirPlaceInTheList() {
        val board = TierList(
            id = 0,
            title = "Films",
            tiers = listOf(
                tier(0, "S").copy(items = listOf(TierItem(3, "Film 3", null), TierItem(1, "Film 1", null))),
                tier(1, "A"),
                tier(2, "B").copy(items = listOf(TierItem(0, "Film 0", null))),
                pool(listOf(TierItem(2, "Film 2", null))),
            ),
        )

        assertEquals(listOf(listOf(3, 1), emptyList(), listOf(0)), rowsOf(answer, board))
    }

    @Test
    fun aRankingWithRows_placesTheCardsInItsOrder() {
        val ordered = ranking.copy(rows = listOf(listOf(3, 2), emptyList(), listOf(0)))

        val board = placedBy(answer, ordered)

        assertEquals(listOf("Film 3", "Film 2"), board.tiers[0].items.map { it.title })
        assertEquals(listOf("Film 0"), board.tiers[2].items.map { it.title })
        assertEquals(listOf("Film 1"), board.tiers.single { it.isPool }.items.map { it.title })
        assertEquals(ordered.rows, rowsOf(answer, board))
    }

    @Test
    fun aRankingKeptWithoutRows_fallsBackToWhereEachCardWent_inTheListsOrder() {
        val board = placedBy(answer, ranking.copy(rows = emptyList()))

        assertEquals(listOf("Film 2", "Film 3"), board.tiers[0].items.map { it.title })
        assertEquals(listOf("Film 0"), board.tiers[2].items.map { it.title })
    }

    @Test
    fun rowsNamingACardTwiceOrOneThatIsNotThere_placeEachRealCardOnce() {
        val board = placedBy(answer, ranking.copy(rows = listOf(listOf(1, 1, 9), listOf(1, 2))))

        assertEquals(listOf("Film 1"), board.tiers[0].items.map { it.title })
        assertEquals(listOf("Film 2"), board.tiers[1].items.map { it.title })
        assertEquals(listOf("Film 0", "Film 3"), board.tiers.single { it.isPool }.items.map { it.title })
    }
}
