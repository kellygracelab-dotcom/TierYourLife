package com.artiuillab.tieryourlife.feature.community.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionDirectoryTest {

    private val cars = Section(id = "cars", name = "Cars", lists30d = 50, approvedAt = 10)
    private val boats = Section(id = "boats", name = "Boats", lists30d = 5, approvedAt = 20)

    @Test
    fun withNothingRead_theNineStand_uncounted() {
        val directory = SectionDirectory()

        assertEquals(listOf("anime", "film_tv", "games", "music", "books", "food", "sport", "people", "other"), directory.shown.map { it.id })
        assertFalse(directory.counted)
    }

    @Test
    fun aMergedId_findsTheSectionItLivesOnAs() {
        val directory = SectionDirectory(sections = listOf(cars), merged = mapOf("autos" to "motors", "motors" to "cars"))

        assertEquals(cars, directory.find("autos"))
        assertEquals("cars", directory.resolve("autos"))
    }

    @Test
    fun aLoopOfMerges_stopsRatherThanSpins() {
        val directory = SectionDirectory(merged = mapOf("a" to "b", "b" to "a"))

        assertEquals("b", directory.resolve("a"))
        assertNull(directory.find("a"))
    }

    @Test
    fun anIdMetAlone_isFound_besideTheList() {
        val directory = SectionDirectory(sections = Section.BUILT_IN, extra = mapOf("cars" to cars))

        assertEquals(cars, directory.find("cars"))
        assertFalse(directory.shown.any { it.id == "cars" })
    }

    @Test
    fun knownAndNeverHeardOf_areSettled_theRestAreNot() {
        val directory = SectionDirectory(unknown = setOf("ghost"))

        assertTrue(directory.settled("food"))
        assertTrue(directory.settled("ghost"))
        assertFalse(directory.settled("cars"))
    }

    @Test
    fun hiddenAndWaitingSections_areNotShown() {
        val directory = SectionDirectory(
            sections = listOf(cars, boats.copy(hidden = true), Section(id = "kites", status = SectionStatus.Pending)),
        )

        assertEquals(listOf("cars"), directory.shown.map { it.id })
    }

    @Test
    fun aFollowMadeHere_movesTheSectionFirst_andMarksIt() {
        val directory = SectionDirectory(sections = listOf(cars, boats, Section.BUILT_IN.last()))

        val after = directory.withFollowing("boats", now = true)

        assertEquals(listOf("boats", "cars", "other"), after.sections.map { it.id })
        assertEquals(true, after.find("boats")?.following)
    }

    @Test
    fun anUnfollow_putsTheSectionBackByItsLists() {
        val directory = SectionDirectory(sections = listOf(boats.copy(following = true), cars))

        val after = directory.withFollowing("boats", now = false)

        assertEquals(listOf("cars", "boats"), after.sections.map { it.id })
        assertEquals(false, after.find("boats")?.following)
    }

    @Test
    fun theOrder_isFollowedThenPinnedThenBusiestThenOldest_otherLast() {
        val other = Section.BUILT_IN.last().copy(following = true)
        val pinnedLate = Section(id = "p2", rank = 2)
        val pinnedEarly = Section(id = "p1", rank = 1)
        val quietOld = Section(id = "q-old", lists30d = 1, approvedAt = 1)
        val quietNew = Section(id = "q-new", lists30d = 1, approvedAt = 9)
        val followed = Section(id = "f", following = true)

        val ordered = SectionOrder.arrange(listOf(other, quietNew, pinnedLate, quietOld, cars, pinnedEarly, followed))

        assertEquals(listOf("f", "p1", "p2", "cars", "q-old", "q-new", "other"), ordered.map { it.id })
    }

    @Test
    fun theBuiltIns_keepTheirOwnOrderAmongThemselves() {
        assertEquals(Section.BUILT_IN, SectionOrder.arrange(Section.BUILT_IN.reversed().reversed()))
    }

    @Test
    fun aName_isTheTranslation_thenItsLanguage_thenTheNameAsTyped() {
        val section = Section(id = "s", name = "Street food", names = mapOf("uk" to "Вулична їжа", "pt-BR" to "Comida de rua"))

        assertEquals("Вулична їжа", section.nameIn("uk-UA"))
        assertEquals("Comida de rua", section.nameIn("pt-BR"))
        assertEquals("Street food", section.nameIn("ja"))
        assertNull(Section(id = "anime").nameIn("en"))
    }
}
