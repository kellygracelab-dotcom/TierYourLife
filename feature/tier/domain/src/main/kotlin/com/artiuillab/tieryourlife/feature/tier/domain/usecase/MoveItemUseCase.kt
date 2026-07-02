package com.artiuillab.tieryourlife.feature.tier.domain.usecase

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

class MoveItemUseCase {
    operator fun invoke(list: TierList, itemId: Long, toTierId: Long): TierList {
        val item = list.tiers.flatMap { it.items }.first { it.id == itemId }
        val tiers = list.tiers.map { tier ->
            if (tier.id == toTierId) {
                if (tier.items.contains(item)) {
                    tier.copy(items = tier.items)
                } else {
                    tier.copy(items = tier.items + item)
                }
            } else {
                tier.copy(items = tier.items.filterNot { it.id == itemId })
            }
        }

        return list.copy(tiers = tiers)
    }
}
