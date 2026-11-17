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
 * A phone held upright shows four, two by two, so the lists start on the
 * first screen (design audit 16.11, option B); the full-width row under them
 * leads to the rest.
 */
internal const val PHONE_SECTIONS = 4

/**
 * The slot rule: up to ten sections, Other last; past ten, the first ten and
 * then the way to all of them, a tile or on a phone the full-width row, with
 * Other left for that screen. An upright phone stops at four the same way.
 * A phone lays tiles two by two, so an odd one out takes the full-width row.
 * The cover scrolls through every one.
 */
internal fun homeTiles(ordered: List<Section>, layout: HomeLayout): HomeTiles {
    val all = ordered.filterNot { it.isOther } + ordered.filter { it.isOther }
    val slots = if (layout == HomeLayout.Portrait) PHONE_SECTIONS else HOME_SECTIONS
    return when {
        layout == HomeLayout.Cover -> HomeTiles(all)
        all.size > slots -> HomeTiles(tiles = all.take(slots), allSections = true, total = all.size)
        layout == HomeLayout.Portrait && all.size % 2 == 1 -> HomeTiles(tiles = all.dropLast(1), row = all.last(), total = all.size)
        else -> HomeTiles(all)
    }
}
