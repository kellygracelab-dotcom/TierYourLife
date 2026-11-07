package com.artiuillab.tieryourlife.feature.tier.board.components.rows

/**
 * Where a picture-less card's name may go, in pixels: [width] across, and
 * [spill] more when one word is too long even at the smallest size.
 */
internal data class CardTitleRoom(val width: Int, val spill: Int, val height: Int)

/** A name laid out: its size in sp, its lines, and how far past [CardTitleRoom.width] it reaches. */
internal data class CardTitle(val size: Float, val lines: List<String>, val extra: Int)

/**
 * The largest size from [largest] down to [smallest] at which [title] breaks
 * between words into lines that all fit. A word too long even at [smallest]
 * takes up to [CardTitleRoom.spill] more width; what still does not fit is
 * left for an ellipsis, and so are the words past the last line.
 */
internal fun fitCardTitle(
    title: String,
    room: CardTitleRoom,
    largest: Float,
    smallest: Float,
    lineHeight: (size: Float) -> Int,
    measure: (text: String, size: Float) -> Int,
): CardTitle {
    val words = title.split(' ', '\n', '\t').filter { it.isNotEmpty() }
    if (words.isEmpty()) return CardTitle(largest, listOf(title), 0)
    fun linesAt(size: Float) = (room.height / lineHeight(size).coerceAtLeast(1)).coerceIn(1, CARD_TITLE_MAX_LINES)

    var size = largest
    while (size >= smallest - EPSILON) {
        val lines = wrapWords(words, room.width) { measure(it, size) }
        if (lines.size <= linesAt(size) && lines.all { measure(it, size) <= room.width }) return CardTitle(size, lines, 0)
        size -= CARD_TITLE_STEP
    }

    val widest = words.maxOf { measure(it, smallest) }
    val extra = (widest - room.width).coerceIn(0, room.spill)
    val lines = wrapWords(words, room.width + extra) { measure(it, smallest) }
    val limit = linesAt(smallest)
    val shown = if (lines.size <= limit) lines else lines.take(limit - 1) + lines.drop(limit - 1).joinToString(" ")
    return CardTitle(smallest, shown, extra)
}

/** One size for the names on a row, so neighbours look alike: the smallest any of them needs. */
internal fun sharedCardTitleSize(titles: List<String>, fit: (String) -> CardTitle): Float? = titles.minOfOrNull { fit(it).size }

/** Words to lines, each as long as fits; a word wider than [width] stands alone. */
internal fun wrapWords(words: List<String>, width: Int, measure: (String) -> Int): List<String> {
    val lines = mutableListOf<String>()
    var line = ""
    for (word in words) {
        val joined = if (line.isEmpty()) word else "$line $word"
        if (line.isEmpty() || measure(joined) <= width) {
            line = joined
        } else {
            lines += line
            line = word
        }
    }
    if (line.isNotEmpty()) lines += line
    return lines
}

internal const val CARD_TITLE_MAX_LINES = 3
internal const val CARD_TITLE_SMALLEST_SP = 9f
private const val CARD_TITLE_STEP = 0.5f
private const val EPSILON = 0.001f
