package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every letter is as wide as the size, and a line 1.25 times as tall, so the arithmetic shows. */
class CardTitleFitTest {

    private fun fit(title: String, room: CardTitleRoom, largest: Float = 11f) = fitCardTitle(
        title = title,
        room = room,
        largest = largest,
        smallest = CARD_TITLE_SMALLEST_SP,
        lineHeight = { (it * 1.25f).toInt() },
        measure = { text, size -> (text.length * size).toInt() },
    )

    @Test
    fun twoShortWords_wrapAtTheLargestSize_ratherThanShrinkOntoOneLine() {
        val fitted = fit("Solid picks", CardTitleRoom(width = 60, spill = 0, height = 100))

        assertEquals(11f, fitted.size)
        assertEquals(listOf("Solid", "picks"), fitted.lines)
        assertEquals(0, fitted.extra)
    }

    @Test
    fun oneLongWord_shrinksUntilItFitsWhole() {
        val fitted = fit("Interstellar", CardTitleRoom(width = 120, spill = 0, height = 100))

        assertEquals(10f, fitted.size)
        assertEquals(listOf("Interstellar"), fitted.lines)
    }

    @Test
    fun aWordTooLongAtTheSmallestSize_leansIntoTheSpill_butNeverShrinksFurther() {
        val fitted = fit("Interstellar", CardTitleRoom(width = 100, spill = 12, height = 100))

        assertEquals(CARD_TITLE_SMALLEST_SP, fitted.size)
        assertEquals(8, fitted.extra)
        assertEquals(listOf("Interstellar"), fitted.lines)
    }

    @Test
    fun aWordTooLongEvenWithTheSpill_takesAllOfIt_andIsLeftWhole_forTheEllipsis() {
        val fitted = fit("Supercalifragilistic", CardTitleRoom(width = 100, spill = 12, height = 100))

        assertEquals(CARD_TITLE_SMALLEST_SP, fitted.size)
        assertEquals(12, fitted.extra)
        assertEquals(listOf("Supercalifragilistic"), fitted.lines)
    }

    @Test
    fun twoLongWords_spillAsFarAsTheWiderOfThem_eachOnItsOwnLine() {
        val fitted = fit("Унесённые призраками", CardTitleRoom(width = 80, spill = 20, height = 100))

        assertEquals(CARD_TITLE_SMALLEST_SP, fitted.size)
        assertEquals(listOf("Унесённые", "призраками"), fitted.lines)
        assertEquals(10, fitted.extra)
    }

    @Test
    fun atMostThreeLines_theRestRidesOnTheLast_forTheEllipsis() {
        val fitted = fit("Eternal Sunshine of the Spotless Mind", CardTitleRoom(width = 80, spill = 0, height = 1_000))

        assertEquals(CARD_TITLE_SMALLEST_SP, fitted.size)
        assertEquals(CARD_TITLE_MAX_LINES, fitted.lines.size)
        assertEquals("Eternal Sunshine of the Spotless Mind", fitted.lines.joinToString(" "))
        assertEquals("of the Spotless Mind", fitted.lines.last())
    }

    @Test
    fun aLowCard_holdsFewerLines_andShrinksToKeepTheWordsInThem() {
        // 13 px a line at 11: two lines fit in 27, so three words shrink until two share a line.
        val fitted = fit("Big Fat Liar", CardTitleRoom(width = 66, spill = 0, height = 27))

        assertTrue(fitted.lines.size <= 2)
        assertTrue(fitted.size < 11f)
        assertEquals("Big Fat Liar", fitted.lines.joinToString(" "))
    }

    @Test
    fun aCapBelowTheLargest_isWhereTheSearchStarts() {
        val fitted = fit("Solid picks", CardTitleRoom(width = 60, spill = 0, height = 100), largest = 9.5f)

        assertEquals(9.5f, fitted.size)
    }

    @Test
    fun aRowSharesTheSmallestSizeAnyOfItsNamesNeeds() {
        val room = CardTitleRoom(width = 120, spill = 0, height = 100)

        assertEquals(10f, sharedCardTitleSize(listOf("Solid picks", "Interstellar")) { fit(it, room) })
        assertEquals(null, sharedCardTitleSize(emptyList()) { fit(it, room) })
    }

    @Test
    fun wrapping_neverBreaksAWord_andALongWordStandsAlone() {
        val lines = wrapWords(listOf("of", "Interstellar", "fame"), width = 8) { it.length }

        assertEquals(listOf("of", "Interstellar", "fame"), lines)
        assertEquals(listOf("a b c", "d"), wrapWords(listOf("a", "b", "c", "d"), width = 5) { it.length })
    }

    @Test
    fun aBlankTitle_isLeftAsItIs() {
        val fitted = fit("  ", CardTitleRoom(width = 60, spill = 0, height = 100))

        assertEquals(listOf("  "), fitted.lines)
    }
}
