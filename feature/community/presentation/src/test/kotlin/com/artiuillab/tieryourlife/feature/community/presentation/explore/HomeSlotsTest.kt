package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Claude Design's slot rule: how many sections Home shows at each window size, and what the last slot becomes. */
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
    fun aPhoneWithMoreThanNine_showsEight_andTheRowLeadsToAll() {
        val sections = theirs(3) + nine
        val home = homeTiles(sections, HomeLayout.Portrait)

        assertEquals(ids(sections.take(8)), ids(home.tiles))
        assertNull(home.row)
        assertTrue(home.allSections)
        assertEquals(12, home.total)
    }

    @Test
    fun aPhoneWithAnEvenNumberThatFits_fillsTheGrid_andLeavesTheRowEmpty() {
        val sections = theirs(7) + nine.last()
        val home = homeTiles(sections, HomeLayout.Portrait)

        assertEquals(8, home.tiles.size)
        assertNull(home.row)
        assertFalse(home.allSections)
    }

    @Test
    fun aMediumWindow_holdsNine_thenEightAndAllSections() {
        assertEquals(ids(nine), ids(homeTiles(nine, HomeLayout.Medium).tiles))

        val more = homeTiles(theirs(1) + nine, HomeLayout.Medium)
        assertEquals(8, more.tiles.size)
        assertTrue(more.allSections)
    }

    @Test
    fun aWideWindow_holdsTen_thenNineAndAllSections() {
        val ten = theirs(1) + nine
        val fits = homeTiles(ten, HomeLayout.Wide)
        assertEquals(ids(ten), ids(fits.tiles))
        assertFalse(fits.allSections)

        val more = homeTiles(theirs(2) + nine, HomeLayout.Wide)
        assertEquals(9, more.tiles.size)
        assertTrue(more.allSections)
    }

    @Test
    fun otherTakesASlotOnlyWhenEverythingFits() {
        val more = homeTiles(nine + theirs(5), HomeLayout.Wide)

        assertFalse(more.tiles.any { it.isOther })
        assertTrue(homeTiles(nine, HomeLayout.Wide).tiles.last().isOther)
    }

    @Test
    fun otherComesLast_whereverTheListPutIt() {
        val home = homeTiles(listOf(nine.last()) + theirs(2), HomeLayout.Medium)

        assertEquals(listOf("s1", "s2", "other"), ids(home.tiles))
    }

    @Test
    fun aShortWindowsRow_holdsTen_likeAWideOne() {
        assertTrue(homeTiles(theirs(2) + nine, HomeLayout.CompactHeight).allSections)
        assertFalse(homeTiles(nine, HomeLayout.CompactHeight).allSections)
    }

    @Test
    fun theCover_scrollsThroughEveryOne() {
        val sections = theirs(6) + nine
        val home = homeTiles(sections, HomeLayout.Cover)

        assertEquals(ids(sections), ids(home.tiles))
        assertFalse(home.allSections)
    }
}
