package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Coordinator, 26.11.2026: on a 320 x 640 phone the new-list button lay on a board; it steps aside while the boards scroll on. */
class ShownWhileScrollingUpTest {

    @Test
    fun scrollingOn_hidesIt() {
        assertFalse(shownAfter(Place(0, 0), Place(0, 40), shown = true))
        assertFalse(shownAfter(Place(1, 200), Place(2, 0), shown = true))
    }

    @Test
    fun scrollingBack_bringsItBack() {
        assertTrue(shownAfter(Place(3, 10), Place(3, 0), shown = false))
        assertTrue(shownAfter(Place(3, 0), Place(2, 90), shown = false))
    }

    @Test
    fun atTheTop_itIsThere() {
        assertTrue(shownAfter(Place(0, 0), Place(0, 0), shown = false))
    }

    @Test
    fun atTheEnd_itIsBack_inTheRoomKeptForIt() {
        assertTrue(shownAfter(Place(10, 0), Place(12, 40), shown = false, atEnd = true))
    }

    @Test
    fun standingStill_keepsWhatItWas() {
        assertFalse(shownAfter(Place(4, 12), Place(4, 12), shown = false))
        assertTrue(shownAfter(Place(4, 12), Place(4, 12), shown = true))
    }
}
