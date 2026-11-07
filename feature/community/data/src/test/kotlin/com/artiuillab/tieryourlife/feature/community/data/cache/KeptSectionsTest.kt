package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.ThemePair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The sections kept on the phone read back as the directory that was kept, less what is worth asking again. */
class KeptSectionsTest {

    private val cars = Section(id = "cars", name = "Cars", names = mapOf("uk" to "Авто"), icon = "directions_car", tint = ThemePair("#111111", "#222222"), listCount = 7)

    @Test
    fun `a directory reads back as it was kept`() {
        val kept = SectionDirectory(
            sections = Section.BUILT_IN + cars,
            extra = mapOf("boats" to Section(id = "boats", name = "Boats")),
            merged = mapOf("autos" to "cars", "bikes" to "cars"),
            mergedTopics = mapOf("bikes" to "t7"),
            mergedNoTopic = setOf("autos"),
            fetchedAtMillis = 42L,
        )

        assertEquals(kept, decodeSections(encodeSections(kept), fetchedAtMillis = 42L))
    }

    @Test
    fun `ids nobody had heard of are not kept, since one may be approved by the next run`() {
        val kept = SectionDirectory(sections = listOf(cars), unknown = setOf("ghost"), fetchedAtMillis = 1L)

        assertEquals(emptySet<String>(), decodeSections(encodeSections(kept), 1L)?.unknown)
    }

    @Test
    fun `this person's own proposals stay their own when read back`() {
        val waiting = Section(id = "kites", name = "Kites", status = com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus.Pending, mine = true, fileUnder = "sport")
        val kept = SectionDirectory(sections = listOf(cars, waiting), fetchedAtMillis = 3L)

        assertEquals(listOf("kites"), decodeSections(encodeSections(kept), 3L)?.waiting?.map { it.id })
    }

    @Test
    fun `what one person follows and proposed is nothing kept for the next`() {
        val waiting = Section(id = "kites", name = "Kites", mine = true, following = true)
        val kept = encodeSections(SectionDirectory(sections = listOf(cars, waiting), fetchedAtMillis = 3L), keptBy = "uid-a")

        assertEquals(listOf("cars", "kites"), decodeSections(kept, 3L, me = "uid-a")?.sections?.map { it.id })
        assertNull(decodeSections(kept, 3L, me = "uid-b"))
        assertNull(decodeSections(kept, 3L, me = null))
    }

    @Test
    fun `a kept list with nothing in it is nothing kept`() {
        assertNull(decodeSections("""{"sections":[]}""", 1L))
    }
}
