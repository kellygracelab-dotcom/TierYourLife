package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.settings.PersonalData
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Firebase ending a session nobody signed out of left the boards under a uid only a sign-in back would show. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LostSessionTest : SyncEngineBench() {

    private val preferences = FakeAppPreferences()
    private val accounts = FakeAccountRepository(initial = ann)
    private val ownership by lazy { BoardOwnership(accounts, database.ownerDao(), database.guestBoardsDao(), preferences) }
    private var forgotten = 0
    private val thePerson = ThePerson(preferences, setOf(PersonalData { forgotten++ }))
    private val bobsAccount = FakeBoards()

    private val session by lazy { LostSession(accounts, database.ownerDao(), ownership, lock, preferences, thePerson) }
    private val merge by lazy {
        RoomBoardMerge(dao, database.ownerDao(), boards, ownership, lock, preferences, accounts, thePerson) { 0L }
    }

    private fun annsEngine() = engine(preferences = preferences, accounts = accounts, ownership = ownership)

    private fun bobsEngine() = engine(preferences = preferences, accounts = accounts, ownership = ownership, api = bobsAccount)

    private suspend fun ownerOf(uid: String) = dao.boardByUid(uid)!!.ownerUid

    private suspend fun annWithTwoBoardsSynced(): Pair<String, String> {
        val films = givenLocalBoard("Films", publishedId = "pub-1", owner = "ann")
        val games = givenLocalBoard("Games", owner = "ann", itemTitle = "Doom")
        session.notice()
        annsEngine().sync()
        boards.kept.clear()
        return films to games
    }

    private suspend fun sessionLost() {
        accounts.accounts.value = Account.Guest
        session.notice()
    }

    private suspend fun signIn(account: Account.SignedIn) {
        accounts.accounts.value = account
        session.notice()
    }

    private fun nothingDeletedIn(account: FakeBoards) = account.server.values.none { it.deleted }

    private val carol = Account.SignedIn(email = "carol@example.com", photoUrl = null, uid = "carol")

    private companion object {
        const val HTTP_TOO_LARGE = 413
    }

    @Test
    fun aLostSession_leavesTheBoardsOnThePhone_andForgetsThePerson() = runBlocking {
        val (films, games) = annWithTwoBoardsSynced()

        sessionLost()

        assertNull(ownerOf(films))
        assertNull(ownerOf(games))
        assertEquals(mapOf("ann" to setOf(films, games)), preferences.heldBoards())
        assertEquals("kept for signing back in", setOf(films, games), dao.syncRecordsOf("ann").map { it.listUid }.toSet())
        assertEquals(1, forgotten)
        assertNull(preferences.lastSignedIn())
    }

    @Test
    fun signingBackIn_findsEachBoardBesideItsCopy_withNoQuestionAndNoDuplicate() = runBlocking {
        val (films, games) = annWithTwoBoardsSynced()
        sessionLost()
        database.tierDao().updateTierListTitle(dao.boardByUid(games)!!.id, "Games I finished")
        val madeSignedOut = givenLocalBoard("Books", owner = null)

        signIn(ann)
        val asked = merge.guestBoards()
        merge.keepHere()
        annsEngine().sync()

        assertEquals("only the board made while signed out", 1, asked)
        assertEquals("ann", ownerOf(films))
        assertEquals("ann", ownerOf(games))
        assertNull(ownerOf(madeSignedOut))
        assertTrue(preferences.heldBoards().isEmpty())
        assertEquals(listOf(games), boards.kept.map { it.first })
        assertEquals(setOf(films, games), boards.server.keys)
        assertTrue(nothingDeletedIn(boards))
    }

    @Test
    fun aRunAsTheAccount_takesItsBoardsBackBeforePlanning_soNoneIsDeletedInIt() = runBlocking {
        annWithTwoBoardsSynced()
        sessionLost()

        // Signed back in with nothing noticing yet: the run alone must not read the boards as thrown away.
        accounts.accounts.value = ann
        annsEngine().sync()

        assertTrue(nothingDeletedIn(boards))
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun boardsNeverSent_comeBackAllTheSame() = runBlocking {
        // Backup off, or refused by the account: no record of them anywhere.
        val films = givenLocalBoard("Films", owner = "ann")
        val trashed = givenLocalBoard("Old", owner = "ann")
        database.tierDao().markTierListsDeleted(listOf(dao.boardByUid(trashed)!!.id), deletedAt = 1)
        session.notice()
        sessionLost()

        signIn(bob)
        val askedAsBob = merge.guestBoards()
        merge.signOutSettlingErase { accounts.signOut() }
        session.notice()
        signIn(ann)

        assertEquals(0, askedAsBob)
        assertEquals("ann", ownerOf(films))
        assertEquals("ann", ownerOf(trashed))
        assertEquals(0, merge.guestBoards())
    }

    @Test
    fun anotherAccount_isNotAskedAboutHeldBoards_andMovingInLeavesThemForTheirOwn() = runBlocking {
        val (films, games) = annWithTwoBoardsSynced()
        sessionLost()
        val books = givenLocalBoard("Books", owner = null)

        signIn(bob)
        val asked = merge.guestBoards()
        merge.moveIn(fromThisPhone = null)

        assertEquals(1, asked)
        assertEquals("bob", ownerOf(books))
        assertNull(ownerOf(films))
        assertNull(ownerOf(games))
        assertEquals(setOf(films, games), dao.syncRecordsOf("ann").map { it.listUid }.toSet())

        merge.signOutSettlingErase { accounts.signOut() }
        signIn(ann)
        annsEngine().sync()
        assertEquals("ann", ownerOf(films))
        assertTrue(nothingDeletedIn(boards))
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun aBrandNewAccount_takesOnlyTheBoardsMadeWithoutOne() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()
        sessionLost()
        val books = givenLocalBoard("Books", owner = null)

        // A first sign-in moves the guest's boards in without a question.
        signIn(bob)
        merge.moveIn(fromThisPhone = null)

        assertEquals("bob", ownerOf(books))
        assertNull(ownerOf(films))
        assertEquals(setOf("ann"), preferences.heldBoards().keys)
    }

    @Test
    fun aBoardKeptHere_isNotTakenByTheRecordAnAccountLeftForIt() = runBlocking {
        // What adopting beside another owner's board leaves: a row for a uid that is not the account's here.
        val kept = givenLocalBoard("Books", owner = null)
        dao.remember(BoardSyncEntity(account = "ann", listUid = kept, revision = 1, fingerprint = "f", syncedAt = 0))
        accounts.accounts.value = Account.Guest

        signIn(ann)
        val asked = merge.guestBoards()
        merge.keepHere()
        annsEngine().sync()

        assertEquals(1, asked)
        assertNull(ownerOf(kept))
    }

    @Test
    fun theLongChain_endsWithNoDeleteInEitherAccount() = runBlocking {
        val (films, games) = annWithTwoBoardsSynced()
        sessionLost()

        signIn(bob)
        assertEquals(0, merge.guestBoards())
        merge.keepHere()
        val bobs = givenLocalBoard("Bob's", owner = "bob")
        bobsEngine().sync()
        sessionLost()
        val books = givenLocalBoard("Books", owner = null)

        signIn(ann)
        assertEquals(1, merge.guestBoards())
        merge.keepHere()
        annsEngine().sync()
        sessionLost()

        signIn(bob)
        assertEquals(1, merge.guestBoards())
        merge.keepHere()
        bobsEngine().sync()

        assertTrue(nothingDeletedIn(boards))
        assertTrue(nothingDeletedIn(bobsAccount))
        assertEquals(setOf(films, games), boards.server.keys)
        assertEquals(setOf(bobs), bobsAccount.server.keys)
        assertEquals("bob", ownerOf(bobs))
        assertNull(ownerOf(books))
        assertEquals(mapOf("ann" to setOf(films, games)), preferences.heldBoards())
    }

    @Test
    fun signingIntoAnotherAccountBeforeTheLossIsNoticed_neverCarriesTheBoardsIn() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()

        // Firebase signed ann out and bob signed in before the watcher had the lock.
        accounts.accounts.value = bob
        merge.hold()
        val asked = merge.guestBoards()
        session.notice()
        merge.moveIn(fromThisPhone = null)

        assertEquals(0, asked)
        assertNull(ownerOf(films))
        assertEquals(setOf("ann"), preferences.heldBoards().keys)
    }

    @Test
    fun movingInBeforeTheLossIsNoticed_leavesTheBoardsToBeHeld() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()

        accounts.accounts.value = bob
        merge.hold()
        merge.guestBoards()
        merge.moveIn(fromThisPhone = null)
        session.notice()

        assertNull(ownerOf(films))
        assertEquals(setOf("ann"), preferences.heldBoards().keys)
    }

    @Test
    fun copyingHeldBoardsIntoAnotherAccount_takesNothingFromTheirOwn() = runBlocking {
        val (films, games) = annWithTwoBoardsSynced()
        val draft = givenLocalBoard("Draft", owner = "ann", itemTitle = "Quake")
        sessionLost()
        database.tierDao().updateTierListTitle(dao.boardByUid(games)!!.id, "Games I finished")
        database.tierDao().markTierListsDeleted(listOf(dao.boardByUid(films)!!.id), deletedAt = 1)

        signIn(bob)
        val asked = merge.heldBoards()
        merge.copyHeldIn(fromThisPhone = null)
        merge.keepHere()
        bobsEngine().sync()

        assertEquals("the trash is not offered", 2, asked)
        val copies = bobsAccount.server.values
        assertEquals(setOf("Games I finished", "Draft"), copies.map { it.title }.toSet())
        assertTrue("under uids of their own", copies.none { it.uid in setOf(films, games, draft) })
        assertNull(ownerOf(films))
        assertNull(ownerOf(games))
        assertEquals(setOf(films, games, draft), preferences.heldBoards().getValue("ann"))
        assertEquals(setOf(films, games), dao.syncRecordsOf("ann").map { it.listUid }.toSet())

        merge.signOutSettlingErase { accounts.signOut() }
        signIn(ann)
        annsEngine().sync()

        assertEquals("ann", ownerOf(draft))
        assertEquals(setOf(films, games, draft), boards.server.keys)
        assertTrue(nothingDeletedIn(boards))
        assertEquals(setOf(films, games, draft), boards.kept.map { it.first }.toSet())
        assertEquals("Games I finished", boards.server.getValue(games).title)
        assertEquals("kept across the reclaim", mapOf("bob" to setOf(games, draft)), preferences.heldBoardsCopied())
    }

    @Test
    fun heldBoardsCopiedOnce_areNotOfferedAgain() = runBlocking {
        annWithTwoBoardsSynced()
        sessionLost()
        signIn(bob)
        merge.copyHeldIn(fromThisPhone = null)
        merge.keepHere()
        merge.signOutSettlingErase { accounts.signOut() }
        session.notice()

        signIn(bob)

        assertEquals(0, merge.heldBoards())
        merge.copyHeldIn(fromThisPhone = null)
        assertEquals("no second set of copies", 2, dao.boardsOf("bob").size)
    }

    @Test
    fun boardsCopiedIntoAnAccount_areNotCopiedAgain_afterTheirOwnReturnsAndIsLostAgain() = runBlocking {
        annWithTwoBoardsSynced()
        sessionLost()
        signIn(bob)
        merge.copyHeldIn(fromThisPhone = null)
        merge.keepHere()
        merge.signOutSettlingErase { accounts.signOut() }
        session.notice()
        signIn(ann)
        sessionLost()

        signIn(bob)
        assertEquals(0, merge.heldBoards())
        merge.copyHeldIn(fromThisPhone = null)
        assertEquals(2, dao.boardsOf("bob").size)

        merge.signOutSettlingErase { accounts.signOut() }
        session.notice()
        signIn(carol)
        assertEquals("another account is still offered them", 2, merge.heldBoards())
    }

    @Test
    fun whatWasCopiedCannotBeWrittenDown_nothingIsCopied() = runBlocking {
        annWithTwoBoardsSynced()
        sessionLost()
        signIn(bob)
        preferences.copiedWriteFails = true

        merge.copyHeldIn(fromThisPhone = null)

        assertTrue(dao.boardsOf("bob").isEmpty())
        assertEquals(2, merge.heldBoards())
    }

    @Test
    fun anAccountDeleted_isNoLongerHeldBackFrom() = runBlocking {
        annWithTwoBoardsSynced()
        sessionLost()
        signIn(bob)
        merge.copyHeldIn(fromThisPhone = null)

        merge.eraseKeepingBoards("bob", erase = { Result.success(Unit) }, signOut = { accounts.signOut() })

        assertTrue(preferences.heldBoardsCopied().isEmpty())
    }

    @Test
    fun aCopyClashingWithTheAccount_isRenamed_andTheHeldBoardKeepsItsName() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()
        sessionLost()
        bobsAccount.server["bobs-films"] = theirBoard("bobs-films", "Films", revision = 1)
        signIn(bob)

        RoomBoardMerge(dao, database.ownerDao(), bobsAccount, ownership, lock, preferences, accounts, thePerson) { 0L }
            .copyHeldIn(fromThisPhone = "(Pixel)")

        assertEquals("Films", dao.boardByUid(films)!!.title)
        assertTrue(dao.boardsOf("bob").any { it.title == withSuffix("Films", "(Pixel)") })
    }

    @Test
    fun theQuestionAboutHeldBoards_countsNeitherTheSignedInAccountsNorTheTrash() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()
        database.tierDao().markTierListsDeleted(listOf(dao.boardByUid(films)!!.id), deletedAt = 1)
        sessionLost()

        signIn(bob)
        assertEquals(1, merge.heldBoards())
        merge.signOutSettlingErase { accounts.signOut() }
        session.notice()
        signIn(ann)
        assertEquals(0, merge.heldBoards())
    }

    @Test
    fun anEraseSettledLater_makesHeldBoardsThePhonesOwn() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()
        sessionLost()
        preferences.setErasureUnderWay(ErasureMark("ann", 0L).encoded())

        merge.settleUnansweredErase()

        assertTrue(preferences.heldBoards().isEmpty())
        assertNull(ownerOf(films))
        assertNull(dao.boardByUid(films)!!.publishedId)
        assertTrue(dao.syncRecordsOf("ann").isEmpty())
        signIn(bob)
        assertEquals("guest boards now, asked about as such", 2, merge.guestBoards())
    }

    @Test
    fun aLossWithAnEraseUnanswered_alsoLetsGoOfWhatWasHeld() = runBlocking {
        val old = givenLocalBoard("Old", publishedId = "pub-old", owner = null)
        preferences.setHeldBoards(mapOf("ann" to setOf(old)))
        annWithTwoBoardsSynced()
        preferences.setErasureUnderWay(ErasureMark("ann", 0L).encoded())

        sessionLost()

        assertTrue(preferences.heldBoards().isEmpty())
        assertNull(ownerOf(old))
        assertNull(dao.boardByUid(old)!!.publishedId)
    }

    @Test
    fun whenWhoseTheBoardsWereCannotBeWritten_nothingIsLetGo_andTheNextLookTriesAgain() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()
        preferences.heldBoardsWriteFails = true

        sessionLost()

        assertEquals("ann", ownerOf(films))
        assertEquals("ann", preferences.lastSignedIn())
        assertTrue(preferences.heldBoards().isEmpty())

        preferences.heldBoardsWriteFails = false
        session.notice()
        assertNull(ownerOf(films))
        assertEquals(setOf("ann"), preferences.heldBoards().keys)
        assertNull(preferences.lastSignedIn())
    }

    @Test
    fun aCopyTheAccountRefuses_keepsTheBoardItStandsFor_untilItGoesUp() = runBlocking {
        // Ann's account holds a uid that is another owner's board here: hers comes down beside it.
        val bobs = givenLocalBoard("Films", owner = "bob")
        boards.server[bobs] = theirBoard(bobs, "Films", revision = 1)
        annsEngine().sync()

        boards.refuseWithCode = HTTP_TOO_LARGE
        annsEngine().sync()
        assertTrue("the old uid stays while its copy is refused", nothingDeletedIn(boards))

        boards.refuseWithCode = null
        annsEngine().sync()
        assertTrue("forgotten once the copy is there", boards.server.getValue(bobs).deleted)
        assertEquals(2, boards.server.size)
        assertEquals("bob", ownerOf(bobs))
    }

    @Test
    fun aSignOutByChoice_isNotALostSession() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()

        merge.signOutSettlingErase { accounts.signOut() }
        session.notice()

        assertEquals("ann", ownerOf(films))
        assertEquals(0, forgotten)
        assertNull(preferences.lastSignedIn())
        assertTrue(preferences.heldBoards().isEmpty())
    }

    @Test
    fun withAnEraseUnanswered_theBoardsAreKeptAsTheDeletedAccounts() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()
        preferences.setErasureUnderWay(ErasureMark("ann", 0L).encoded())

        sessionLost()

        assertNull(ownerOf(films))
        assertNull(dao.boardByUid(films)!!.publishedId)
        assertTrue(dao.syncRecordsOf("ann").isEmpty())
        assertTrue(preferences.heldBoards().isEmpty())
        assertNull(preferences.erasureUnderWay())
        assertEquals(1, forgotten)
    }

    @Test
    fun theSameAccountStillSignedIn_changesNothing() = runBlocking {
        val (films, _) = annWithTwoBoardsSynced()

        session.notice()

        assertEquals("ann", ownerOf(films))
        assertEquals("ann", preferences.lastSignedIn())
        assertEquals(0, forgotten)
    }
}
