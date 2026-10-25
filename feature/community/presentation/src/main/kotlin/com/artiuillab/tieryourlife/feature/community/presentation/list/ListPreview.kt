package com.artiuillab.tieryourlife.feature.community.presentation.list

/** What the card showed before its list was opened: enough to draw the screen before the answer. */
data class ListPreview(
    val title: String,
    /** Null for a list that stands without an author. */
    val authorName: String?,
    val itemCount: Int,
    val tierColors: List<String>,
)

/** More than this and a row wraps, which the placeholder never does; the real board may. */
internal const val SKELETON_TILES_PER_ROW = 5

/**
 * How many grey cards each row gets while the real ones load: the count spread
 * over the rows from the top. The board that lands will not match card for
 * card and does not need to; it needs to be about the same height, so nothing
 * jumps when it does.
 */
internal fun skeletonTiles(itemCount: Int, rows: Int): List<Int> {
    if (rows <= 0) return emptyList()
    val shown = itemCount.coerceIn(0, rows * SKELETON_TILES_PER_ROW)
    val each = shown / rows
    val extra = shown % rows
    return List(rows) { index -> each + if (index < extra) 1 else 0 }
}
