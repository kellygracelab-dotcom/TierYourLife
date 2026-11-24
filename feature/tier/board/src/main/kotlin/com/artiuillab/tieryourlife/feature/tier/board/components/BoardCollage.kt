package com.artiuillab.tieryourlife.feature.tier.board.components

import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * One of your boards' first pictures glued into one, on the phone only, so a
 * tile loads one picture instead of six. Laid out as the website lays out a
 * published list's collage (_coordination/cover-collage-2026-11-23/BRIEF.md),
 * but [width] by [height] of the tile it is shown in: drawn for its own frame,
 * nothing is cut off by another shape.
 */
data class BoardCollage(
    val boardId: String,
    val pictures: List<String>,
    val focus: List<CoverFocus?>,
    val width: Int,
    val height: Int,
) {
    /** Changes with anything that changes the picture: which pictures, their order, their focus, the size. */
    val key: String by lazy {
        sha256(
            buildString {
                append(boardId).append('\n')
                pictures.forEachIndexed { index, picture ->
                    append(picture).append('\t').append(focus[index]?.fingerprintParts?.joinToString(",") ?: "-").append('\n')
                }
                append(width).append('x').append(height)
            },
        )
    }

    /** What every picture of this board at this size is named from, so the one it replaces is found and dropped. */
    val slot: String get() = "${sha256(boardId).take(BOARD_CHARS)}-${width}x$height"

    val fileName: String get() = "$slot-${key.take(KEY_CHARS)}.webp"

    private companion object {
        const val BOARD_CHARS = 16
        const val KEY_CHARS = 32
    }
}

/** A collage of a board's first distinct pictures, or null where one picture or none is all there is to show. */
fun boardCollageOf(
    boardId: String,
    previewImages: List<String>,
    pictureFocus: Map<String, CoverFocus>,
    width: Int,
    height: Int,
): BoardCollage? {
    val pictures = previewImages.distinct().take(MOSAIC_CELLS)
    if (pictures.size < MIN_COLLAGE_PICTURES || width <= 0 || height <= 0) return null
    return BoardCollage(boardId, pictures, pictures.map { pictureFocus[it] }, width, height)
}

/** One picture's place in a collage, in its pixels; right and bottom are exclusive. */
data class CollageCell(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** The mosaic's rows, with transparent gaps between the cells and none at the edges. */
fun collageCells(count: Int, width: Int, height: Int): List<CollageCell> {
    val gap = collageGap(width)
    val rows = mosaicRows(count)
    return rows.flatMapIndexed { row, perRow ->
        val (top, bottom) = span(row, rows.size, height, gap)
        List(perRow) { column ->
            val (left, right) = span(column, perRow, width, gap)
            CollageCell(left, top, right, bottom)
        }
    }
}

/** The website's 2 pixels on 480, at whatever width this one is drawn, never less than one. */
fun collageGap(width: Int): Int = max(1, (width.toFloat() * SITE_GAP / SITE_WIDTH).roundToInt())

private fun span(index: Int, of: Int, length: Int, gap: Int): Pair<Int, Int> {
    val step = (length + gap).toDouble() / of
    return (index * step).roundToInt() to ((index + 1) * step).roundToInt() - gap
}

private fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

private const val MIN_COLLAGE_PICTURES = 2
private const val SITE_GAP = 2f
private const val SITE_WIDTH = 480f
