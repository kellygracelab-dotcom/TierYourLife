package com.artiuillab.tieryourlife.feature.account.data.repository

import com.google.firebase.auth.FirebaseAuthInvalidUserException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/** Read as gone, a deleted account's boards become this phone's own; read wrongly, they turn up twice. */
class AccountExistenceTest {

    @Test
    fun aUserTheServerNoLongerKnows_isGone() {
        assertEquals(false, existenceAfter(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no such user")))
    }

    @Test
    fun aDisabledUser_isNotTakenForGone() {
        assertNull(existenceAfter(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "disabled")))
    }

    @Test
    fun noAnswer_tellsNothing() {
        assertNull(existenceAfter(IOException("offline")))
    }
}
