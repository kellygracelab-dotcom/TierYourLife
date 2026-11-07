package com.artiuillab.tieryourlife.feature.community.presentation.feed.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorInitialTest {

    @Test
    fun aNameStartingWithAnEmojiShowsTheWholeEmoji() {
        assertEquals("🦊", initialOf("🦊 Fox"))
        assertEquals("🇺🇦", initialOf("🇺🇦Olena"))
    }

    @Test
    fun aNameStartingWithALetterShowsItUppercase() {
        assertEquals("A", initialOf("ann"))
    }
}
