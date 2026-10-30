package com.artiuillab.tieryourlife.feature.tier.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class PublishedReconciliationTest {

    @Test
    fun aListTheSameAccountNoLongerHas_isForgotten() {
        val reconciled = reconcilePublished(
            claimed = setOf("a1", "a2"),
            inAccount = setOf("a1"),
            account = "ann",
            lastAccount = "ann",
            owners = emptyMap(),
        )

        assertEquals(setOf("a2"), reconciled.forget)
        assertEquals(mapOf("a1" to "ann"), reconciled.owners)
    }

    // The bug: signing into a second account wiped every id the first had published.
    @Test
    fun signingIntoAnotherAccount_keepsTheFirstAccountsLists() {
        val reconciled = reconcilePublished(
            claimed = setOf("a1", "b1"),
            inAccount = setOf("b1"),
            account = "bob",
            lastAccount = "ann",
            owners = mapOf("a1" to "ann"),
        )

        assertEquals(emptySet<String>(), reconciled.forget)
        assertEquals(mapOf("a1" to "ann", "b1" to "bob"), reconciled.owners)
    }

    @Test
    fun anIdNeverSeenInAnyAccount_goesToTheAccountSignedInBeforeTheSwitch() {
        val reconciled = reconcilePublished(
            claimed = setOf("a9"),
            inAccount = emptySet(),
            account = "bob",
            lastAccount = "ann",
            owners = emptyMap(),
        )

        assertEquals(emptySet<String>(), reconciled.forget)
        assertEquals(mapOf("a9" to "ann"), reconciled.owners)
    }

    @Test
    fun theOtherAccountsListsStayOnEveryLaterRun_too() {
        val first = reconcilePublished(setOf("a1"), emptySet(), account = "bob", lastAccount = "ann", owners = emptyMap())
        val second = reconcilePublished(setOf("a1"), emptySet(), account = "bob", lastAccount = "bob", owners = first.owners)

        assertEquals(emptySet<String>(), second.forget)
        assertEquals(mapOf("a1" to "ann"), second.owners)
    }

    @Test
    fun backInTheFirstAccount_itsTakenDownListIsForgotten_andTheSecondsKept() {
        val reconciled = reconcilePublished(
            claimed = setOf("a1", "b1"),
            inAccount = emptySet(),
            account = "ann",
            lastAccount = "bob",
            owners = mapOf("a1" to "ann", "b1" to "bob"),
        )

        assertEquals(setOf("a1"), reconciled.forget)
        assertEquals(mapOf("b1" to "bob"), reconciled.owners)
    }

    // Before anything was recorded: the account signed in is the only one there has been.
    @Test
    fun theFirstCheck_forgetsWhatTheAccountDoesNotHave() {
        val reconciled = reconcilePublished(setOf("x"), emptySet(), account = "ann", lastAccount = null, owners = emptyMap())

        assertEquals(setOf("x"), reconciled.forget)
    }

    @Test
    fun anIdNoBoardClaimsAnyMore_isNotKept() {
        val reconciled = reconcilePublished(emptySet(), setOf("a1"), account = "ann", lastAccount = "ann", owners = mapOf("old" to "bob"))

        assertEquals(emptyMap<String, String>(), reconciled.owners)
    }
}
