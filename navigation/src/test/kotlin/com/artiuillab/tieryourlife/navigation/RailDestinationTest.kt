package com.artiuillab.tieryourlife.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Which rail item lights up for a route. The strings are what type-safe
 * navigation actually produces -- the class name followed by its arguments --
 * because a test against a tidier string would pass for a rule that fails on
 * the real one.
 */
class RailDestinationTest {

    private val lists = "com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route.TierLists" +
        "?community={community}&makeBoard={makeBoard}"
    private val settings = "com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route.Settings"
    private val board = "com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route.TierDetail/{tierListId}"

    @Test
    fun theHomeRoute_isListsOrCommunity_byWhatItWasOpenedOn() {
        assertEquals(RailDestination.Lists, railDestinationFor(lists, onCommunity = false))
        assertEquals(RailDestination.Community, railDestinationFor(lists, onCommunity = true))
    }

    @Test
    fun settings_isItsOwnItem() {
        assertEquals(RailDestination.Settings, railDestinationFor(settings, onCommunity = false))
    }

    // A rail with nothing lit is honest about being somewhere it did not take
    // you. Lighting Lists on a board would say the board is the list screen.
    @Test
    fun anywhereTheRailDidNotTakeYou_lightsNothing() {
        assertNull(railDestinationFor(board, onCommunity = false))
        assertNull(railDestinationFor(null, onCommunity = false))
    }

    // The community flag is only meaningful on the home route. On any other it
    // is noise from a stale read, and must not turn Settings into Community.
    @Test
    fun theCommunityFlag_meansNothingOffTheHomeRoute() {
        assertEquals(RailDestination.Settings, railDestinationFor(settings, onCommunity = true))
        assertNull(railDestinationFor(board, onCommunity = true))
    }
}
