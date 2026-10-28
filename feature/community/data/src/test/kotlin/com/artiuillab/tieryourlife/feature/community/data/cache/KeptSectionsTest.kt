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
            merged = mapOf("autos" to "cars"),
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
    fun `a kept list with nothing in it is nothing kept`() {
        assertNull(decodeSections("""{"sections":[]}""", 1L))
    }
}
