package com.artiuillab.tieryourlife.feature.tier.domain.lists

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

fun TierList.itemLayout(): Map<Long, List<Long>> = tiers.associate { it.id to it.items.map { card -> card.id } }

/** A layout may reorder or move cards, never add, remove or duplicate them. */
fun sameLayoutCards(expected: Map<Long, List<Long>>, target: Map<Long, List<Long>>): Boolean {
    val before = expected.values.flatten()
    val after = target.values.flatten()
    return expected.keys == target.keys &&
        before.size == before.toSet().size &&
        after.size == before.size &&
        after.toSet() == before.toSet()
}
