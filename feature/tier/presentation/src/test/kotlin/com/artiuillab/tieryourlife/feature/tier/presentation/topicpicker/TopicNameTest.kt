package com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopicNameTest {

    @Test
    fun aName_isCollapsedAndTrimmed() {
        assertEquals("Oscar  snubs".replace("  ", " "), cleanTopicName("  Oscar   snubs \n"))
    }

    @Test
    fun tooShort_tooLong_andPunctuationAlone_areNoName() {
        assertNull(cleanTopicName("a"))
        assertNull(cleanTopicName("x".repeat(MAX_TOPIC_NAME_LENGTH + 1)))
        assertNull(cleanTopicName("!!! ..."))
        assertEquals("x".repeat(MAX_TOPIC_NAME_LENGTH), cleanTopicName("x".repeat(MAX_TOPIC_NAME_LENGTH)))
    }

    @Test
    fun theLookupWord_isTheFirstWordWithSomeWeight() {
        assertEquals("Oscar", topicLookupWord("An Oscar list, 2025"))
        assertEquals("Pokémon", topicLookupWord("Pokémon: every starter"))
        assertNull(topicLookupWord("A B"))
    }
}
