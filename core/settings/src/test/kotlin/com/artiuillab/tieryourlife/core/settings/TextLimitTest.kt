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
        assertEquals("aa", "aa\uD83D\uDE00".takeWhole(3))
    }

    @Test
    fun anEmojiThatFitsIsKept() {
        assertEquals("aa\uD83D\uDE00", "aa\uD83D\uDE00b".takeWhole(4))
    }
}
