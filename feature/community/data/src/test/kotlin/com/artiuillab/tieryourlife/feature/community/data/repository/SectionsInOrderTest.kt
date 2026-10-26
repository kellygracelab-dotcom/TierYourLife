package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the sections answer becomes on the phone, whatever order or gaps the server sent. */
class SectionsInOrderTest {

    @Test
    fun `all nine come back in the app's order whatever the server sent`() {
        val answer = listOf(SectionDto(id = "food", newThisWeek = 22), SectionDto(id = "anime", listCount = 5))

        val sections = sectionsInOrder(answer)

        assertEquals(ListCategory.entries, sections.map { it.section })
        assertEquals(5, sections.first { it.section == ListCategory.Anime }.listCount)
        assertEquals(22, sections.first { it.section == ListCategory.Food }.newThisWeek)
        assertEquals(0, sections.first { it.section == ListCategory.Games }.newThisWeek)
    }

    @Test
    fun `an id the app does not know is dropped`() {
        val sections = sectionsInOrder(listOf(SectionDto(id = "cars", listCount = 9)))

        assertEquals(9, sections.size)
        assertTrue(sections.all { it.listCount == 0 })
    }

    @Test
    fun `the collage keeps four web pictures at most`() {
        val pictures = listOf("https://a", "http://b", "https://c", "https://d", "https://e", "https://f")

        val games = sectionsInOrder(listOf(SectionDto(id = "games", collage = pictures))).first { it.section == ListCategory.Games }

        assertEquals(listOf("https://a", "https://c", "https://d", "https://e"), games.collage)
    }
}
