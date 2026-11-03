package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.readIn
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** The page as a reader in [languageTag] reads it: the author's board, a chosen reader's and their own. */
internal fun CommunityListUiState.readIn(languageTag: String): CommunityListUiState {
    if (this !is CommunityListUiState.Success) return this
    val words = translations.wordsIn(languageTag)
    if (words.isEmpty()) return this
    return copy(list = list.readIn(words), mine = mine.readIn(words), theirs = theirs.readIn(words))
}

/**
 * A board handed back from the screen, read in the reader's language, with
 * [mine]'s tiers and [list]'s cards in their own words again, by id: the
 * state holds a list as written, whatever language it was read in.
 */
internal fun TierList.inWordsOf(mine: TierList, list: TierList): TierList {
    val tiersById = mine.tiers.associateBy { it.id }
    val cardsById = list.tiers.flatMap { it.items }.associateBy { it.id }
    return copy(
        title = mine.title,
        tiers = tiers.map { tier ->
            val own = tiersById[tier.id]
            tier.copy(
                label = own?.label ?: tier.label,
                caption = if (own != null) own.caption else tier.caption,
                items = tier.items.map { card -> cardsById[card.id]?.let { card.copy(title = it.title) } ?: card },
            )
        },
    )
}
