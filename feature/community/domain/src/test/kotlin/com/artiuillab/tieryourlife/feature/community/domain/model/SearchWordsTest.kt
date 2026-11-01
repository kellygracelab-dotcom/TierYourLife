package com.artiuillab.tieryourlife.feature.community.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchWordsTest {

    private val anime = Section(id = "anime", name = "Anime", names = mapOf("ru" to "Аниме", "fr" to "Animé"))
    private val filmTv = Section(id = "film_tv", name = "Film & TV", names = mapOf("ru" to "Кино и сериалы"))

    @Test
    fun `folds case and accents but keeps й`() {
        assertEquals("звездные войны", SearchWords.fold("Звёздные Войны"))
        assertEquals("pokemon", SearchWords.fold("Pokémon"))
    }

    @Test
    fun `a section is found by its name in another language`() {
        assertTrue(anime.matchesSearch("аниме"))
        assertTrue(anime.matchesSearch("anime"))
    }

    @Test
    fun `a section is found by the start of any of its words, in any order`() {
        assertTrue(filmTv.matchesSearch("сериал"))
        assertTrue(filmTv.matchesSearch("tv fil"))
    }

    @Test
    fun `every typed word has to be there`() {
        assertFalse(filmTv.matchesSearch("film games"))
        assertFalse(anime.matchesSearch("!!"))
    }

    @Test
    fun `the id stands in for a section with no name yet`() {
        assertTrue(Section(id = "board_games").matchesSearch("board"))
    }
}
