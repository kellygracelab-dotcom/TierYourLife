package com.artiuillab.tieryourlife.feature.account.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Read as gone, a deleted account's boards become this phone's own; read wrongly, they turn up twice. */
class AccountExistenceTest {

    @Test
    fun aUserTheServerNoLongerKnows_isGone() {
        assertEquals(false, existenceAfter("ERROR_USER_NOT_FOUND"))
    }

    @Test
    fun aDisabledUser_isNotTakenForGone() {
        assertNull(existenceAfter("ERROR_USER_DISABLED"))
    }

    @Test
    fun noAnswer_tellsNothing() {
        assertNull(existenceAfter(null))
    }
}
