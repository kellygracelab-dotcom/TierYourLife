package com.artiuillab.tieryourlife.feature.tier.board.share

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardPictureLineTest {

    @Test
    fun aLineEndsAtTheLastSpaceThatFits() {
        assertEquals(8, lineEnd("Films of 2026", 9))
    }

    @Test
    fun aLineWithNoSpaceEndsBeforeAnEmojiItCannotHoldWhole() {
        val text = "Films🇺🇦"
        assertEquals(5, lineEnd(text, 8))
        assertEquals(5, lineEnd(text, 6))
    }

    @Test
    fun aLineTooNarrowForTheFirstEmojiStillTakesIt() {
        val family = "👨‍👩‍👧"
        assertEquals(family.length, lineEnd("${family}x", 0))
    }

    @Test
    fun aLineThatFitsIsTheWholeText() {
        assertEquals(3, lineEnd("abc", 3))
    }
}
