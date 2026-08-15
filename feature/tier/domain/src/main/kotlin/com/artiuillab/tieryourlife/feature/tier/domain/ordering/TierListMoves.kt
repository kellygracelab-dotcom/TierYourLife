package com.artiuillab.tieryourlife.feature.tier.domain.ordering

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

fun TierList.withItemMoved(itemId: Long, toTierId: Long, toPosition: Int): TierList {
    val movedItem = tiers.firstNotNullOfOrNull { tier -> tier.items.firstOrNull { it.id == itemId } }
        ?: return this
    if (tiers.none { it.id == toTierId }) return this

    val withoutItem = tiers.map { tier -> tier.copy(items = tier.items.filterNot { it.id == itemId }) }
    val targetSize = withoutItem.first { it.id == toTierId }.items.size
    val insertAt = toPosition.coerceIn(0, targetSize)

    return copy(
        tiers = withoutItem.map { tier ->
            if (tier.id == toTierId) {
                tier.copy(items = tier.items.toMutableList().apply { add(insertAt, movedItem) })
            } else {
                tier
            }
        },
    )
}
