package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.feature.community.domain.model.Section

/** What Home's tiles hold at one window size. */
internal data class HomeTiles(
    /** The section tiles, in order. */
    val tiles: List<Section>,
    /** On a phone, the section on the full-width row under the grid. */
    val row: Section? = null,
    /** The last slot leads to every section: a tile, or on a phone the full-width row. */
    val allSections: Boolean = false,
    /** How many sections there are in all, for that slot to say. */
    val total: Int = tiles.size,
)

/**
 * How many sections Home has room for (Claude Design): a phone's eight and
 * its row, nine at 600–839, ten from 840 and in a short window's row. The
 * cover scrolls through every one.
 */
internal val HomeLayout.slots: Int?
    get() = when (this) {
        HomeLayout.Portrait, HomeLayout.Medium -> 9
        HomeLayout.Wide, HomeLayout.CompactHeight -> 10
        HomeLayout.Cover -> null
    }

/**
 * The slot rule: everything when it fits, Other last; otherwise the first
 * sections and, in the last slot, the way to all of them, with Other left for
 * that screen. A phone lays tiles two by two, so an odd one out takes the
 * full-width row: with the nine built-ins, Other, as it always has.
 */
internal fun homeTiles(ordered: List<Section>, layout: HomeLayout): HomeTiles {
    val rest = ordered.filterNot { it.isOther }
    val all = rest + ordered.filter { it.isOther }
    val slots = layout.slots
    return when {
        slots == null -> HomeTiles(all)
        all.size > slots -> HomeTiles(tiles = rest.take(slots - 1), allSections = true, total = all.size)
        layout == HomeLayout.Portrait && all.size % 2 == 1 -> HomeTiles(tiles = all.dropLast(1), row = all.last(), total = all.size)
        else -> HomeTiles(all)
    }
}
