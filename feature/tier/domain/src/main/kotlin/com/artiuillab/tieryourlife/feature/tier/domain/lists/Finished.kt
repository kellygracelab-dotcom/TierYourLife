package com.artiuillab.tieryourlife.feature.tier.domain.lists

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Every card on the board, the pool's included. */
fun TierList.cardCount(): Int = tiers.sumOf { it.items.size }

/** Nothing left in the pool: "N of N placed". */
fun TierList.allPlaced(): Boolean = tiers.none { it.isPool && it.items.isNotEmpty() }

/** This move placed the last card: the board was not all placed before it and is now. */
fun TierList.finishedFrom(before: TierList): Boolean = allPlaced() && !before.allPlaced()
