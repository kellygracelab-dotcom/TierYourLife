package com.artiuillab.tieryourlife.feature.tier.domain.lists

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/**
 * What order somebody's own boards come in, and which of them are shown.
 *
 * Pure, so the rules can be argued with in a unit test rather than through a
 * screen. The screen decides how it looks; this decides what it holds.
 */
object BoardOrder {

    /**
     * Starred boards first, then the rest, each in the chosen order.
     *
     * Whether the two are *drawn* as separate groups is a separate question --
     * see [shouldGroup] -- but the order is the same either way, so switching
     * a filter on and off never reshuffles what stays on screen.
     */
    fun arrange(
        lists: List<TierList>,
        sort: BoardSort,
        filters: BoardFilters = BoardFilters(),
    ): BoardArrangement {
        val kept = lists.filter { filters.keeps(it) }
        val (starred, rest) = kept.partition { it.favouritedAt != null }
        return BoardArrangement(
            favourites = starred.sortedByDescending { it.favouritedAt },
            rest = rest.sortedWith(sort.comparator),
        )
    }

    /**
     * Whether the two groups get headings.
     *
     * Without them the top two cards read as the two newest, and the sort
     * control sitting directly above says so: somebody switches to Oldest,
     * sees the same two on top, and concludes the sort is broken.
     *
     * Off while a filter or a search is narrowing things down. There the
     * screen is an answer to a question, and pinning something above the
     * answer is a second question nobody asked.
     */
    fun shouldGroup(arrangement: BoardArrangement, narrowed: Boolean): Boolean =
        !narrowed && arrangement.favourites.isNotEmpty() && arrangement.rest.isNotEmpty()
}

data class BoardArrangement(
    val favourites: List<TierList> = emptyList(),
    val rest: List<TierList> = emptyList(),
) {
    /** Everything, in order, for the places that do not draw the groups apart. */
    val all: List<TierList> get() = favourites + rest
}

enum class BoardSort {
    Newest,
    Oldest,
    ;

    /**
     * By when the board was last touched, and by its id where that is not
     * known -- a board made before the app recorded the time still has to sit
     * somewhere, and the id is the order they were made in.
     */
    internal val comparator: Comparator<TierList>
        get() = when (this) {
            Newest -> compareByDescending<TierList> { it.editedAt ?: 0L }.thenByDescending { it.id }
            Oldest -> compareBy<TierList> { it.editedAt ?: 0L }.thenBy { it.id }
        }
}

/**
 * Deliberately without a "favourites" value: starred boards are already
 * pinned to the top, and a filter that showed only them would contradict the
 * pinning the moment both were on.
 */
data class BoardFilters(
    val category: ListCategory? = null,
    val published: PublishedFilter? = null,
) {
    val any: Boolean get() = category != null || published != null

    internal fun keeps(list: TierList): Boolean {
        if (category != null && list.category != category) return false
        return when (published) {
            null -> true
            PublishedFilter.Public -> list.publishedId != null
            PublishedFilter.Private -> list.publishedId == null
        }
    }
}

enum class PublishedFilter { Public, Private }
