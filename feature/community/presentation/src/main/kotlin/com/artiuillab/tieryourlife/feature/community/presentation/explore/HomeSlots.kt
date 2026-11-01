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
 * How many sections Home shows before the way to all of them; the site shows
 * twenty. Which ones is the server's order: followed first, then the ones the
 * owner pinned, then the busiest.
 */
internal const val HOME_SECTIONS = 10

/**
 * The slot rule: up to ten sections, Other last; past ten, the first ten and
 * then the way to all of them, a tile or on a phone the full-width row, with
 * Other left for that screen. A phone lays tiles two by two, so an
 * odd one out takes the full-width row: with the nine built-ins, Other, as it
 * always has. The cover scrolls through every one.
 */
internal fun homeTiles(ordered: List<Section>, layout: HomeLayout): HomeTiles {
    val all = ordered.filterNot { it.isOther } + ordered.filter { it.isOther }
    return when {
        layout == HomeLayout.Cover -> HomeTiles(all)
        all.size > HOME_SECTIONS -> HomeTiles(tiles = all.take(HOME_SECTIONS), allSections = true, total = all.size)
        layout == HomeLayout.Portrait && all.size % 2 == 1 -> HomeTiles(tiles = all.dropLast(1), row = all.last(), total = all.size)
        else -> HomeTiles(all)
    }
}
