package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository

internal const val POOL = 99L

/** A board from tier ids to card ids, the pool last; a card's title is its id. */
internal fun board(vararg tiers: Pair<Long, List<Long>>, id: Long = 1): TierList = TierList(
    id = id,
    title = "Films",
    tiers = tiers.map { (tierId, cards) ->
        Tier(
            id = tierId,
            label = if (tierId == POOL) "Pool" else "T$tierId",
            colorLight = "#B03A32",
            colorDark = "#F1948C",
            items = cards.map { TierItem(it, "Card $it", null) },
            isPool = tierId == POOL,
        )
    },
)

internal fun TierList.ids(): List<Pair<Long, List<Long>>> = tiers.map { tier -> tier.id to tier.items.map { it.id } }

/** A repository holding one board that moves its cards the way the database does, and remembers every move. */
internal class MovingTierRepository(
    var board: TierList,
    private val rest: TierRepository = RecordingTierRepository(),
) : TierRepository by rest {

    val moves = mutableListOf<Move>()

    override suspend fun getTierListById(id: Long): TierList? = board.takeIf { it.id == id }

    override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        moves += Move(itemId, toTierId, toPosition)
        board = board.withItemMoved(itemId, toTierId, toPosition)
    }
}
