package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The slot rule: up to ten sections on Home, four on an upright phone, in the server's order, then the way to all of them. */
class HomeSlotsTest {

    private val nine = Section.BUILT_IN
    private fun theirs(count: Int) = (1..count).map { Section(id = "s$it", name = "S$it") }
    private fun ids(sections: List<Section>) = sections.map { it.id }

    @Test
    fun aPhoneWithTheNine_showsTheFirstFour_andTheRowLeadsToAll() {
        val home = homeTiles(nine, HomeLayout.Portrait)

        assertEquals(ids(nine.take(4)), ids(home.tiles))
        assertNull(home.row)
        assertTrue(home.allSections)
        assertEquals(9, home.total)
    }

    @Test
    fun aPhoneWithManyMore_stillShowsFour_inTheServersOrder() {
        val sections = theirs(141) + nine
        val home = homeTiles(sections, HomeLayout.Portrait)

        assertEquals(ids(sections.take(4)), ids(home.tiles))
        assertNull(home.row)
        assertTrue(home.allSections)
        assertEquals(150, home.total)
    }

    @Test
    fun aPhoneWithFour_showsThemAll_withoutTheWayToAll() {
        val home = homeTiles(theirs(4), HomeLayout.Portrait)

        assertEquals(4, home.tiles.size)
        assertFalse(home.allSections)
    }

    @Test
    fun aPhoneWithThree_putsTheOddOneOnTheFullWidthRow() {
        val home = homeTiles(theirs(2) + nine.last(), HomeLayout.Portrait)

        assertEquals(listOf("s1", "s2"), ids(home.tiles))
        assertEquals("other", home.row?.id)
        assertFalse(home.allSections)
    }

    @Test
    fun onAPhone_otherIsLeftForAllSections() {
        val home = homeTiles(listOf(nine.last()) + theirs(5), HomeLayout.Portrait)

        assertFalse(home.tiles.any { it.isOther })
        assertTrue(home.allSections)
    }

    @Test
    fun widerWindows_holdTen_thenTenAndAllSections() {
        listOf(HomeLayout.Medium, HomeLayout.Wide, HomeLayout.CompactHeight).forEach { layout ->
            val fits = homeTiles(theirs(1) + nine, layout)
            assertEquals(10, fits.tiles.size)
            assertFalse(fits.allSections)

            val more = homeTiles(theirs(2) + nine, layout)
            assertEquals(10, more.tiles.size)
            assertTrue(more.allSections)
            assertEquals(11, more.total)
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
