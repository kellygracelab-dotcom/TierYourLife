package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** The engine's card order for a board: tiers top to bottom, each left to right, then the pool. */
internal fun TierList.playOrder(): List<Tier> = tiers.filterNot { it.isPool } + tiers.filter { it.isPool }

/** The cards a game takes: those of [scope]'s tiers, or of the whole board when it is null, in the engine's order. */
internal fun TierList.cardsIn(scope: Collection<Long>?): List<TierItem> =
    playOrder().filter { scope == null || it.id in scope }.flatMap { it.items }.distinctBy { it.id }

/**
 * Played cards lead each result tier. Unplayed cards keep their original order
 * after them, or go to the pool after its existing cards. Every id occurs once.
 */
internal fun TierList.arrangedBy(placement: Map<Long, List<Long>>, unplayed: UnplayedPolicy = UnplayedPolicy.Keep): TierList {
    val byId = tiers.flatMap { it.items }.associateBy { it.id }
    val tierIds = tiers.map { it.id }.toSet()
    val placed = placement.filterKeys { it in tierIds }
    val assigned = mutableSetOf<Long>()
    val arriving = tiers.associate { tier ->
        tier.id to placed[tier.id].orEmpty().filter { it in byId && assigned.add(it) }.map { byId.getValue(it) }
    }
    val remaining = mutableSetOf<Long>()
    val leftovers = tiers.associate { tier -> tier.id to tier.items.filter { it.id !in assigned && remaining.add(it.id) } }
    val pool = tiers.firstOrNull { it.isPool }?.id
    val pooling = unplayed == UnplayedPolicy.Pool && pool != null
    val toPool = if (pooling) (tiers.filter { it.isPool } + playOrder().filterNot { it.isPool }).flatMap { leftovers[it.id].orEmpty() } else emptyList()
    return copy(
        tiers = tiers.map { tier ->
            val kept = if (!pooling) leftovers[tier.id].orEmpty() else if (tier.id == pool) toPool else emptyList()
            tier.copy(items = arriving[tier.id].orEmpty() + kept)
        },
    )
}

/**
 * Undo of an arrangement: every card back where [before] had it. A card added
 * since stays in its tier after them; one deleted since is not brought back,
 * and nor is a tier.
 */
internal fun TierList.restoredTo(before: TierList): TierList {
    val present = tiers.flatMap { it.items }.associateBy { it.id }
    val tierIds = tiers.map { it.id }.toSet()
    val oldTiers = before.tiers.filter { it.id in tierIds }.associateBy { it.id }
    val returning = oldTiers.values.flatMap { it.items }.map { it.id }.filter { it in present }.toSet()
    return copy(
        tiers = tiers.map { tier ->
            val back = oldTiers[tier.id]?.items.orEmpty().mapNotNull { present[it.id] }
            tier.copy(items = back + tier.items.filterNot { it.id in returning })
        },
    )
}

/** One step of the board's own move, so an applied result goes through the same door as a drag. */
internal data class Move(val itemId: Long, val tierId: Long, val position: Int)

/**
 * The moves that turn [from] into [to], which hold the same cards: each tier
 * in turn is filled from the left, and a card already in its place is not
 * touched. Once a tier is done, nothing later takes from in front of its end.
 */
internal fun movesBetween(from: TierList, to: TierList): List<Move> {
    var board = from
    val moves = mutableListOf<Move>()
    for (tier in to.tiers) {
        tier.items.forEachIndexed { position, item ->
            val there = board.tiers.firstOrNull { it.id == tier.id }?.items?.getOrNull(position)
            if (there?.id != item.id) {
                moves += Move(item.id, tier.id, position)
                board = board.withItemMoved(item.id, tier.id, position)
            }
        }
    }
    return moves
}
