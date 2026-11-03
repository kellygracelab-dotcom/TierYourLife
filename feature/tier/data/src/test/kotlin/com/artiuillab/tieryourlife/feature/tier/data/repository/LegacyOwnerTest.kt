package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyOwnerTest {

    @Test
    fun aBoardOneAccountAgreedOn_isThatAccounts_whoeverIsSignedIn() {
        assertEquals("ann", legacyOwner(listOf(record("ann", at = 1)), signedIn = "bob"))
        assertEquals("ann", legacyOwner(listOf(record("ann", at = 1)), signedIn = null))
    }

    // The owner's phone: Ann's boards went up into Bob's account while he was signed in.
    @Test
    fun aBoardSeveralAccountsAgreedOn_isTheSignedInOnes_ifItIsAmongThem() {
        val records = listOf(record("ann", at = 5), record("bob", at = 1))

        assertEquals("bob", legacyOwner(records, signedIn = "bob"))
    }

    @Test
    fun aBoardSeveralAccountsAgreedOn_isTheLatestOnes_otherwise() {
        val records = listOf(record("ann", at = 5), record("bob", at = 9))

        assertEquals("bob", legacyOwner(records, signedIn = "cat"))
        assertEquals("bob", legacyOwner(records, signedIn = null))
    }

    @Test
    fun aBoardNoAccountAgreedOn_isTheSignedInOnes() {
        assertEquals("ann", legacyOwner(emptyList(), signedIn = "ann"))
    }

    @Test
    fun aBoardNoAccountAgreedOn_withNobodySignedIn_staysThisPhonesOwn() {
        assertNull(legacyOwner(emptyList(), signedIn = null))
    }

    private fun record(account: String, at: Long) =
        BoardSyncEntity(account = account, listUid = "films", revision = 1, fingerprint = "f", syncedAt = at)
}
