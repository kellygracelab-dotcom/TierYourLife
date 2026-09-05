package com.artiuillab.tieryourlife.feature.tier.domain.lists

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Order and filters for somebody's own boards. Pure, so the rules are argued with in a unit test. */
object BoardOrder {

    /**
     * Starred first, then the rest, each in the chosen order. Whether the two
     * are drawn apart is [shouldGroup]'s question; the order is the same either
     * way, so a filter never reshuffles what stays on screen.
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
     * Without headings the top cards read as the newest, and the sort control
     * above says so: switch to Oldest, same cards on top, sort looks broken.
     * Off while a filter or search narrows: the screen is an answer, and
     * pinning something above it is a second question nobody asked.
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

    /** By last touch, then by id: a board made before the time was recorded still has to sit somewhere. */
    internal val comparator: Comparator<TierList>
        get() = when (this) {
            Newest -> compareByDescending<TierList> { it.editedAt ?: 0L }.thenByDescending { it.id }
            Oldest -> compareBy<TierList> { it.editedAt ?: 0L }.thenBy { it.id }
        }
}

/** No "favourites" value: starred boards are already pinned, and a filter for them would contradict the pinning. */
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
