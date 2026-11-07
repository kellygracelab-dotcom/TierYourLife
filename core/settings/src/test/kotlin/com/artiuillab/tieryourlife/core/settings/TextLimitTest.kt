package com.artiuillab.tieryourlife.core.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TextLimitTest {

    @Test
    fun aShortTextIsKept() {
        assertEquals("abc", "abc".takeWhole(5))
    }

    @Test
    fun anEmojiAtTheCutIsDroppedWhole() {
        assertEquals("aa", "aa😀".takeWhole(3))
    }

    @Test
    fun anEmojiThatFitsIsKept() {
        assertEquals("aa😀", "aa😀b".takeWhole(4))
    }

    @Test
    fun aFamilyIsNotCutAtItsJoiner() {
        val family = "👨‍👩‍👧"
        assertEquals("a", "a$family".takeWhole(1 + 5))
        assertEquals("a", "a$family".takeWhole(1 + 3))
        assertEquals("a$family", "a${family}b".takeWhole(1 + family.length))
    }

    @Test
    fun aFlagIsNotCutBetweenItsTwoLetters() {
        val ukraine = "🇺🇦"
        val poland = "🇵🇱"
        assertEquals("a", "a$ukraine".takeWhole(3))
        assertEquals("a$ukraine", "a$ukraine$poland".takeWhole(1 + 4 + 2))
    }

    @Test
    fun aSkinToneAndAVariationSelectorStayWithTheirEmoji() {
        val wave = "👋🏽"
        val heart = "❤️"
        assertEquals("a", "a$wave".takeWhole(3))
        assertEquals("a", "a$heart".takeWhole(2))
    }

    @Test
    fun anAccentStaysWithItsLetter() {
        assertEquals("ab", "abé".takeWhole(3))
    }

    @Test
    fun aFirstLetterLongerThanTheLimit_isStillCut_ratherThanLost() {
        val marked = "a" + "\u0301".repeat(90)
        assertEquals(marked.take(80), marked.takeWhole(80))

        val chain = List(20) { "👨" }.joinToString("\u200D")
        assertEquals(chain.take(39), chain.takeWhole(40))
    }

    @Test
    fun theFirstWholeIsTheWholeEmoji() {
        val family = "👨‍👩‍👧"
        assertEquals(family, "${family}Ann".firstWhole())
        assertEquals("🇺🇦", "🇺🇦🇵🇱".firstWhole())
        assertEquals("A", "Ann".firstWhole())
        assertEquals("", "".firstWhole())
    }
}
