package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The slot rule, as on the site: up to twenty sections on Home, then the way to all of them. */
class HomeSlotsTest {

    private val nine = Section.BUILT_IN
    private fun theirs(count: Int) = (1..count).map { Section(id = "s$it", name = "S$it") }
    private fun ids(sections: List<Section>) = sections.map { it.id }

    @Test
    fun aPhoneWithTheNine_keepsOtherOnTheFullWidthRow_asItAlwaysHas() {
        val home = homeTiles(nine, HomeLayout.Portrait)

        assertEquals(ids(nine.dropLast(1)), ids(home.tiles))
        assertEquals("other", home.row?.id)
        assertFalse(home.allSections)
    }

    @Test
    fun aPhoneWithTwenty_showsThemAll_withoutTheWayToAll() {
        val sections = theirs(11) + nine
        val home = homeTiles(sections, HomeLayout.Portrait)

        assertEquals(20, home.tiles.size)
        assertNull(home.row)
        assertFalse(home.allSections)
    }

    @Test
    fun aPhoneWithMoreThanTwenty_showsTwenty_andTheRowLeadsToAll() {
        val sections = theirs(14) + nine
        val home = homeTiles(sections, HomeLayout.Portrait)

        assertEquals(ids(sections.take(20)), ids(home.tiles))
        assertNull(home.row)
        assertTrue(home.allSections)
        assertEquals(23, home.total)
    }

    @Test
    fun aPhoneWithAnOddNumberThatFits_putsTheLastOneOnTheRow() {
        val sections = theirs(10) + nine
        val home = homeTiles(sections, HomeLayout.Portrait)

        assertEquals(18, home.tiles.size)
        assertEquals("other", home.row?.id)
        assertFalse(home.allSections)
    }

    @Test
    fun widerWindows_holdTwenty_thenTwentyAndAllSections() {
        listOf(HomeLayout.Medium, HomeLayout.Wide, HomeLayout.CompactHeight).forEach { layout ->
            val fits = homeTiles(theirs(11) + nine, layout)
            assertEquals(20, fits.tiles.size)
            assertFalse(fits.allSections)

            val more = homeTiles(theirs(12) + nine, layout)
            assertEquals(20, more.tiles.size)
            assertTrue(more.allSections)
            assertEquals(21, more.total)
        }
    }

    @Test
    fun otherTakesASlotOnlyWhenEverythingFits() {
        val more = homeTiles(nine + theirs(15), HomeLayout.Wide)

        assertFalse(more.tiles.any { it.isOther })
        assertTrue(homeTiles(nine, HomeLayout.Wide).tiles.last().isOther)
    }

    @Test
    fun otherComesLast_whereverTheListPutIt() {
        val home = homeTiles(listOf(nine.last()) + theirs(2), HomeLayout.Medium)

        assertEquals(listOf("s1", "s2", "other"), ids(home.tiles))
    }

    @Test
    fun theCover_scrollsThroughEveryOne() {
        val sections = theirs(16) + nine
        val home = homeTiles(sections, HomeLayout.Cover)

        assertEquals(ids(sections), ids(home.tiles))
        assertFalse(home.allSections)
    }
}
