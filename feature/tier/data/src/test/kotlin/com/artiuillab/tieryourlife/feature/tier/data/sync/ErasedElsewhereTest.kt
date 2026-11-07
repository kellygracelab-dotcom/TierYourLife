package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.network.ErasedAccounts
import com.artiuillab.tieryourlife.core.settings.AccountErasedNotice
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.settings.PersonalData
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The proxy said ACCOUNT_ERASED: another phone deleted the account this one is still signed into. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ErasedElsewhereTest : SyncEngineBench() {

    private val preferences = FakeAppPreferences()
    private val accounts = FakeAccountRepository(initial = ann)
    private val notice = AccountErasedNotice(preferences)
    private var forgotten = 0

    private fun watcher() = ErasedElsewhere(
        ErasedAccounts(),
        RoomBoardMerge(
            dao,
            database.ownerDao(),
            boards,
            BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences),
            lock,
            preferences,
            accounts,
            ThePerson(preferences, setOf(PersonalData { forgotten++ })),
        ) { 0L },
        notice,
    )

    private suspend fun ownerOf(uid: String) = dao.boardByUid(uid)!!.ownerUid

    @Test
    fun theSignedInAccountErased_signsOutAndKeepsItsBoardsHere() = runBlocking {
        val films = givenLocalBoard("Films", publishedId = "pub-1", owner = "ann")
        val others = givenLocalBoard("Bob's games", owner = "bob")
        engine(account = ann).sync()
        preferences.setLastSignedIn("ann")

        watcher().leave("ann")

        assertEquals(Account.Guest, accounts.current())
        assertNull("the phone's own now, as after deleting it here", ownerOf(films))
        assertNull(dao.boardByUid(films)!!.publishedId)
        assertEquals("bob", ownerOf(others))
        assertTrue(dao.syncRecordsOf("ann").isEmpty())
        assertNull("not a session lost: nothing is held for it", preferences.lastSignedIn())
        assertTrue(preferences.heldBoards().isEmpty())
        assertEquals(1, forgotten)
        assertTrue(notice.pending.value)
        assertTrue("still there after a restart", preferences.accountErasedNoticePending())
    }

    // Answered after a sign-out, or to a token another account's request carried.
    @Test
    fun anotherAccountErased_changesNothing() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")

        watcher().leave("bob")

        assertEquals(ann, accounts.current())
        assertEquals("ann", ownerOf(films))
        assertEquals(0, forgotten)
        assertFalse(notice.pending.value)
    }

    @Test
    fun aGuestHearingIt_changesNothing() = runBlocking {
        accounts.accounts.value = Account.Guest

        watcher().leave("ann")

        assertEquals(0, forgotten)
        assertFalse(notice.pending.value)
    }

    @Test
    fun theNotice_goesOnceRead() {
        notice.raise()
        notice.dismiss()

        assertFalse(notice.pending.value)
        assertFalse(AccountErasedNotice(preferences).pending.value)
    }
}
