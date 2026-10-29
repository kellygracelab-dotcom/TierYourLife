package com.artiuillab.tieryourlife.feature.community.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Whose list it is: the server's word first, then a matching uid, never an empty one. */
class OwnListTest {

    @Test
    fun anAnonymousListTheServerSaysIsMine_isMine() {
        assertTrue(isOwnList(mine = true, authorUid = "", myUid = "me"))
    }

    @Test
    fun aNamedListWithMyUid_isMine_evenBeforeTheServerSaysSo() {
        assertTrue(isOwnList(mine = false, authorUid = "me", myUid = "me"))
    }

    @Test
    fun somebodyElsesList_isNotMine() {
        assertFalse(isOwnList(mine = false, authorUid = "olena", myUid = "me"))
    }

    @Test
    fun somebodyElsesAnonymousList_isNotMine() {
        assertFalse(isOwnList(mine = false, authorUid = "", myUid = "me"))
    }

    @Test
    fun anEmptyUid_matchesNobody_notEvenSomebodyWithoutOne() {
        assertFalse(isOwnList(mine = false, authorUid = "", myUid = ""))
        assertFalse(isOwnList(mine = false, authorUid = "me", myUid = null))
    }
}
