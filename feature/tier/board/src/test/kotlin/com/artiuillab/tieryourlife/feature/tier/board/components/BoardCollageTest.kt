package com.artiuillab.tieryourlife.feature.tier.board.components

import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What a board's glued picture is drawn from, and when it has to be drawn again. */
class BoardCollageTest {

    private val pictures = listOf("a", "b", "c", "d")

    private fun collage(
        board: String = "7",
        images: List<String> = pictures,
        focus: Map<String, CoverFocus> = emptyMap(),
        width: Int = 400,
        height: Int = 296,
    ) = requireNotNull(boardCollageOf(board, images, focus, width, height))

    @Test
    fun theSameBoard_isTheSameKey() {
        assertEquals(collage().key, collage().key)
        assertEquals(collage().fileName, collage().fileName)
    }

    @Test
    fun changingTheFirstCards_theirOrder_focusOrShape_changesTheKey() {
        val key = collage().key

        assertNotEquals(key, collage(images = listOf("a", "b", "c", "e")).key)
        assertNotEquals(key, collage(images = listOf("b", "a", "c", "d")).key)
        assertNotEquals(key, collage(focus = mapOf("a" to CoverFocus(0.2f, 0.3f, 1.5f))).key)
        assertNotEquals(key, collage(width = 300, height = 300).key)
        assertNotEquals(key, collage(board = "8").key)
    }

    @Test
    fun cardsPastTheSixth_andRepeats_doNotChangeIt() {
        val six = listOf("a", "b", "c", "d", "e", "f")

        assertEquals(collage(images = six).key, collage(images = six + "g").key)
        assertEquals(collage().key, collage(images = listOf("a", "a", "b", "c", "d")).key)
    }

    @Test
    fun onePicture_isNoCollage() {
        assertNull(boardCollageOf("7", listOf("a", "a"), emptyMap(), 400, 296))
        assertNull(boardCollageOf("7", emptyList(), emptyMap(), 400, 296))
        assertNull(boardCollageOf("7", pictures, emptyMap(), 0, 0))
    }

    @Test
    fun anOlderPictureOfTheBoard_sharesItsSlot() {
        val now = collage()
        val before = collage(images = listOf("z", "b"))

        assertEquals(now.slot, before.slot)
        assertNotEquals(now.slot, collage(width = 56, height = 56).slot)
        assertNotEquals(now.slot, collage(board = "8").slot)
    }

    @Test
    fun cells_areTheSitesRows_withGapsBetweenAndNoneAtTheEdges() {
        val width = 480
        val height = 300
        val gap = collageGap(width)
        assertEquals(2, gap)

        val five = collageCells(5, width, height)
        assertEquals(listOf(3, 2), five.groupBy { it.top }.values.map { it.size })
        five.groupBy { it.top }.values.forEach { row ->
            assertEquals(0, row.first().left)
            assertEquals(width, row.last().right)
            row.zipWithNext { left, right -> assertEquals(gap, right.left - left.right) }
        }
        assertEquals(0, five.first().top)
        assertEquals(height, five.last().bottom)
        assertEquals(gap, five.last().top - five.first().bottom)

        assertEquals(listOf(2), collageCells(2, width, height).groupBy { it.top }.values.map { it.size })
        assertEquals(listOf(2, 2), collageCells(4, width, height).groupBy { it.top }.values.map { it.size })
        assertEquals(listOf(3, 3), collageCells(6, width, height).groupBy { it.top }.values.map { it.size })
    }

    @Test
    fun theGap_scalesWithTheWidth_butNeverVanishes() {
        assertEquals(1, collageGap(146))
        assertEquals(2, collageGap(480))
        assertEquals(4, collageGap(960))
    }
}
