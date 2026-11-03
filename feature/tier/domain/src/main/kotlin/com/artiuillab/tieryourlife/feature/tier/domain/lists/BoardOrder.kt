package com.artiuillab.tieryourlife.feature.tier.domain.lists

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary

/** Order and filters for somebody's own boards. Pure, so the rules are argued with in a unit test. */
object BoardOrder {

    /**
     * Starred first, then the rest, each in the chosen order. Whether the two
     * are drawn apart is [shouldGroup]'s question; the order is the same either
     * way, so a filter never reshuffles what stays on screen.
     */
    fun arrange(
        lists: List<TierListSummary>,
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
    val favourites: List<TierListSummary> = emptyList(),
    val rest: List<TierListSummary> = emptyList(),
) {
    /** Everything, in order, for the places that do not draw the groups apart. */
    val all: List<TierListSummary> get() = favourites + rest
}

enum class BoardSort {
    Newest,
    Oldest,
    ;

    /** By last touch, then by id: a board made before the time was recorded still has to sit somewhere. */
    internal val comparator: Comparator<TierListSummary>
        get() = when (this) {
            Newest -> compareByDescending<TierListSummary> { it.editedAt ?: 0L }.thenByDescending { it.id }
            Oldest -> compareBy<TierListSummary> { it.editedAt ?: 0L }.thenBy { it.id }
        }
}

/** No "favourites" value: starred boards are already pinned, and a filter for them would contradict the pinning. */
data class BoardFilters(
    /** A section id. */
    val category: String? = null,
    val published: PublishedFilter? = null,
) {
    val any: Boolean get() = category != null || published != null

    internal fun keeps(list: TierListSummary): Boolean {
        if (category != null && list.category != category) return false
        return when (published) {
            null -> true
            PublishedFilter.Public -> list.isPublic
            PublishedFilter.Private -> !list.isPublic
        }
    }
}

enum class PublishedFilter { Public, Private }
