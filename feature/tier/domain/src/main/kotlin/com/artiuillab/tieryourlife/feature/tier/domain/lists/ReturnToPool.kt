package com.artiuillab.tieryourlife.feature.tier.domain.lists

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Existing pool first, then ranked tiers in reading order. Cards and tiers remain intact. */
fun TierList.returningAllToPool(): TierList {
    val pool = tiers.firstOrNull { it.isPool } ?: return this
    val seen = mutableSetOf<Long>()
    val cards = (tiers.filter { it.isPool } + tiers.filterNot { it.isPool })
        .flatMap { it.items }.filter { seen.add(it.id) }
    return copy(tiers = tiers.map { it.copy(items = if (it.id == pool.id) cards else emptyList()) })
}
