package com.artiuillab.tieryourlife.feature.aistudio.domain.naming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardTitleFromPromptTest {

    @Test
    fun `strips a leading indefinite article and capitalizes the first letter`() {
        val result = cardTitleFromPrompt("A neon-lit Tokyo street in the rain")

        assertEquals("Neon-lit Tokyo street in the rain", result)
    }

    @Test
    fun `strips a leading definite article and keeps an acronym's casing intact`() {
        val result = cardTitleFromPrompt("the retro VHS cover with bold type")

        assertEquals("Retro VHS cover with bold type", result)
    }

    @Test
    fun `strips a leading an article`() {
        val result = cardTitleFromPrompt("an eagle soaring high above the canyon")

        assertEquals("Eagle soaring high above the canyon", result)
    }

    @Test
    fun `blank input returns an empty string`() {
        assertEquals("", cardTitleFromPrompt("   "))
    }

    @Test
    fun `empty input returns an empty string`() {
        assertEquals("", cardTitleFromPrompt(""))
    }

    @Test
    fun `a prompt without a leading article is only capitalized`() {
        val result = cardTitleFromPrompt("sunset over the mountains")

        assertEquals("Sunset over the mountains", result)
    }

    @Test
    fun `an already capitalized first letter is left unchanged`() {
        val result = cardTitleFromPrompt("Sunset over mountains")

        assertEquals("Sunset over mountains", result)
    }

    @Test
    fun `collapses runs of internal whitespace, including tabs and newlines, to one space`() {
        val result = cardTitleFromPrompt("A   moody\n\tforest   at dusk")

        assertEquals("Moody forest at dusk", result)
    }

    @Test
    fun `trims trailing punctuation and the spaces around it`() {
        val result = cardTitleFromPrompt("A calm lake at dawn!!!  ")

        assertEquals("Calm lake at dawn", result)
    }

    @Test
    fun `a prompt that is only punctuation returns an empty string`() {
        assertEquals("", cardTitleFromPrompt("!!! ..."))
    }

    @Test
    fun `a prompt no longer than 40 characters after article removal is not truncated`() {
        val prompt = "alpha bravo charlie delta echo foxtrot"
        assertEquals(38, prompt.length)

        val result = cardTitleFromPrompt(prompt)

        assertEquals("Alpha bravo charlie delta echo foxtrot", result)
    }

    @Test
    fun `a prompt longer than 40 characters is cut at the last word boundary within the limit`() {
        val prompt = "alpha bravo charlie delta echo foxtrot golf hotel india"
        assertEquals(55, prompt.length)

        val result = cardTitleFromPrompt(prompt)

        assertEquals("Alpha bravo charlie delta echo foxtrot", result)
        assertTrue(result.length <= 40)
    }

    @Test
    fun `a single word longer than 40 characters is hard cut at 40`() {
        val prompt = "x".repeat(45)

        val result = cardTitleFromPrompt(prompt)

        assertEquals(40, result.length)
        assertEquals("X" + "x".repeat(39), result)
    }

    @Test
    fun `truncation and article removal combine and the result never exceeds 40 characters`() {
        val prompt = "the alpha bravo charlie delta echo foxtrot golf hotel india"

        val result = cardTitleFromPrompt(prompt)

        assertTrue(result.length <= 40)
        assertEquals("Alpha bravo charlie delta echo foxtrot", result)
    }
}
