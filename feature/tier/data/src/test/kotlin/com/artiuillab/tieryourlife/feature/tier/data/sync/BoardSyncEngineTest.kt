package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomTierRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** What a sync run does with the steps the plan decides. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BoardSyncEngineTest : SyncEngineBench() {

    @Test
    fun aGuest_sendsNothing() = runBlocking {
        givenLocalBoard("Films", owner = null)

        val report = engine(account = Account.Guest).sync()

        assertFalse(report.signedIn)
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun withBackupOff_nothingIsSent() = runBlocking {
        givenLocalBoard("Films")

        val report = engine(backUp = false).sync()

        assertFalse(report.signedIn)
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun aNewBoard_goesUpOnce_andIsRemembered() = runBlocking {
        val uid = givenLocalBoard("Films")

        val first = engine().sync()
        engine().sync()

        assertEquals(1, first.carried)
        assertEquals(listOf(uid), boards.kept.map { it.first })
        assertNull(boards.kept.single().second.basedOn)
        assertEquals(1, dao.allSyncRecords().single { it.listUid == uid }.revision)
    }

    @Test
    fun aBoardFromAnotherPhone_arrivesWithItsTiersAndCards() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4)

        engine().sync()

        val arrived = dao.boardByUid("remote-1")!!
        assertEquals("Games", arrived.title)
        assertEquals(listOf("S"), dao.tiersOf(arrived.id).map { it.label })
        assertEquals(listOf("Doom"), dao.itemsOf(arrived.id).map { it.title })
        assertEquals(4, dao.allSyncRecords().single { it.listUid == "remote-1" }.revision)
    }

    // Seen on a phone: a board started on the site held a game whose uid a
    // board here already had, and it never arrived.
    @Test
    fun aBoardWhoseCardsShareUidsWithOneHere_stillArrives() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4)
        engine().sync()
        boards.server["remote-2"] = theirBoard("remote-1", title = "More games", revision = 1).copy(uid = "remote-2")

        val report = engine().sync()

        assertEquals(0, report.refused)
        val first = dao.itemsOf(dao.boardByUid("remote-1")!!.id).single()
        val second = dao.itemsOf(dao.boardByUid("remote-2")!!.id).single()
        assertEquals("remote-1-item", first.uid)
        assertEquals("Doom", second.title)
        assertTrue(second.uid != first.uid)
    }

    // A board written down as anything but what is stored here goes straight
    // back up, and to the site that is a change it did not make.
    @Test
    fun aCardsCredit_goesUpWithIt_andComesDownOnAnotherPhone() = runBlocking {
        givenLocalBoard("Animals", credit = "© Jane Doe, CC BY")
        boards.server["remote-1"] = theirBoard("remote-1", title = "Birds", revision = 2).let { board ->
            board.copy(items = board.items.map { it.copy(credit = "© Ann Lee, CC BY-SA") })
        }

        engine().sync()

        assertEquals(listOf("© Jane Doe, CC BY"), boards.kept.single().second.items.map { it.credit })
        val arrived = dao.boardByUid("remote-1")!!
        assertEquals(listOf("© Ann Lee, CC BY-SA"), dao.itemsOf(arrived.id).map { it.credit })
    }

    @Test
    fun aBoardJustAdopted_isNotSentBack_evenOnceItsPictureArrives() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4, pictureId = "poster.jpg")
        val engine = engine(vault = Holding("poster.jpg"), connection = Unmetered)

        engine.sync()
        val second = engine.sync()

        assertTrue(images.holds("poster.jpg"))
        assertEquals(0, second.carried)
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun aBoardJustRestored_isNotSentBack() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4)
        val engine = engine()
        engine.sync()
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games, renamed on the site", revision = 5, fingerprint = "theirs-5")

        engine.sync()
        val second = engine.sync()

        assertEquals("Games, renamed on the site", dao.boardByUid("remote-1")!!.title)
        assertEquals(0, second.carried)
        assertTrue(boards.kept.isEmpty())
    }

    // The list screen and a returning connection start a run each at launch.
    // The second read the board before the first restored it, took the restore
    // for an edit here and sent the board back as a new revision.
    @Test
    fun aRestoreByARunStartedMeanwhile_isNotSentBack() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4)
        val engine = engine()
        engine.sync()
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games, renamed on the site", revision = 5, fingerprint = "theirs-5")
        var other: Job? = null
        boards.onIndex = {
            boards.onIndex = {}
            other = launch { engine.sync() }
            // Long enough for the other run to finish, were it let in.
            withTimeoutOrNull(OTHER_RUN_MS) { other?.join() }
        }

        engine.sync()
        other?.join()

        assertEquals("Games, renamed on the site", dao.boardByUid("remote-1")!!.title)
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun anEditMadeDuringARun_goesUpOnce() = runBlocking {
        val uid = givenLocalBoard("Films")
        val engine = engine()
        engine.sync()
        dao.renameBoard(uid, "Films, edited")
        boards.onIndex = {
            boards.onIndex = {}
            dao.renameBoard(uid, "Films, edited again")
        }

        engine.sync()
        engine.sync()

        assertEquals(listOf("Films", "Films, edited again"), boards.kept.map { it.second.title })
    }

    // The answer to our last write never arrived. Taking our own board back
    // as a copy would leave a duplicate every time the app is killed mid-sync.
    @Test
    fun aRefusalCarryingOurOwnBoard_takesTheMissedRevisionAndMakesNoCopy() = runBlocking {
        val uid = givenLocalBoard("Films")
        boards.refuseNextWith = theirBoard(uid, title = "Films", revision = 7, fingerprint = fingerprintOf(uid))

        engine().sync()

        assertEquals(1, dao.allBoards().size)
        assertEquals(7, dao.allSyncRecords().single { it.listUid == uid }.revision)
    }

    @Test
    fun aRefusalCarryingADifferentBoard_keepsBothAndSendsOursOnTop() = runBlocking {
        val uid = givenLocalBoard("Films")
        boards.refuseNextWith = theirBoard(uid, title = "Films (tablet)", revision = 3, deviceName = "Tablet")

        engine().sync()

        val copy = dao.allBoards().single { it.uid != uid }
        assertEquals("Films (tablet)", copy.title)
        assertEquals("Tablet", copy.arrivedFrom)
        assertEquals(uid, boards.kept.last().first)
        assertEquals(3, boards.kept.last().second.basedOn)
    }

    // A board the account keeps turning down is not backed up, and saying it
    // was would hide the stuck banner from the one person who needs it.
    @Test
    fun aWriteRefusedForAnotherReason_isCountedAsRefused_notCarried() = runBlocking {
        val uid = givenLocalBoard("Films")
        boards.refuseWithCode = 413

        val report = engine().sync()

        assertEquals(1, report.refused)
        assertEquals(0, report.carried)
        assertTrue(dao.allSyncRecords().none { it.listUid == uid })
    }

    @Test
    fun aStepThatFails_isCountedAndTheRunCarriesOn() = runBlocking {
        givenLocalBoard("Films")
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 1)
        boards.keepFails = true

        val report = engine().sync()

        assertEquals(1, report.refused)
        assertEquals(1, report.carried)
        assertNotNull(dao.boardByUid("remote-1"))
    }

    // Signing into a second account used to wipe every id the first had published off its boards.
    @Test
    fun anotherAccountsSync_leavesTheFirstAccountsPublishedListsAlone() = runBlocking {
        givenLocalBoard("Films", publishedId = "ann-1", owner = "ann")
        val preferences = FakeAppPreferences()
        val ann = Account.SignedIn(email = "ann@example.com", photoUrl = null, uid = "ann")
        val bob = Account.SignedIn(email = "bob@example.com", photoUrl = null, uid = "bob")

        engine(account = ann, preferences = preferences, community = Publishing("ann-1")).sync()
        // Signed into Bob's account, who publishes a board of his own from this phone.
        givenLocalBoard("Games", publishedId = "bob-1", owner = "bob")
        engine(account = bob, preferences = preferences, community = Publishing("bob-1")).sync()
        engine(account = bob, preferences = preferences, community = Publishing("bob-1")).sync()

        assertEquals(setOf("ann-1", "bob-1"), dao.allBoards().mapNotNull { it.publishedId }.toSet())
    }

    @Test
    fun aListTheSameAccountTookDown_isForgotten() = runBlocking {
        givenLocalBoard("Films", publishedId = "ann-1", owner = "ann")
        val preferences = FakeAppPreferences()
        val ann = Account.SignedIn(email = "ann@example.com", photoUrl = null, uid = "ann")

        engine(account = ann, preferences = preferences, community = Publishing("ann-1")).sync()
        engine(account = ann, preferences = preferences, community = Publishing()).sync()

        assertEquals(emptyList<String>(), dao.allBoards().mapNotNull { it.publishedId })
    }

    // The review's case: Bob empties the trash on this phone, and Ann's board
    // in it is not his to throw away, here or in her account.
    @Test
    fun anotherAccountEmptyingTheTrash_leavesTheFirstAccountsBoard() = runBlocking {
        val uid = givenLocalBoard("Films", owner = "ann")
        val annBoards = FakeBoards()
        engine(account = ann, api = annBoards).sync()

        throwAway(uid, signedInAs = "bob")
        engine(account = ann, api = annBoards).sync()

        assertEquals(false, annBoards.server[uid]?.deleted)
        assertNotNull("still in Ann's trash", dao.boardByUid(uid)?.deletedAt)
    }

    @Test
    fun aBoardThrownAwayUnderTheSameAccount_leavesItsAccount() = runBlocking {
        val uid = givenLocalBoard("Films", owner = "ann")
        val annBoards = FakeBoards()
        engine(account = ann, api = annBoards).sync()

        throwAway(uid, signedInAs = "ann")
        engine(account = ann, api = annBoards).sync()

        assertEquals(true, annBoards.server[uid]?.deleted)
    }

    // The owner's phone sent Ann's boards into Bob's account while he was signed in.
    @Test
    fun anAccountsBoards_neverGoUpIntoAnother() = runBlocking {
        val uid = givenLocalBoard("Films", owner = "ann")
        val annBoards = FakeBoards()
        engine(account = ann, api = annBoards).sync()
        val bobBoards = FakeBoards()

        val report = engine(account = bob, api = bobBoards).sync()

        assertEquals(0, report.carried)
        assertTrue(bobBoards.kept.isEmpty())
        assertTrue(dao.syncRecordsOf("bob").isEmpty())
        assertEquals(listOf(uid), dao.syncRecordsOf("ann").map { it.listUid })
    }

    @Test
    fun boardsMadeWithoutAnAccount_goUpOnlyOnceMovedIn() = runBlocking {
        val uid = givenLocalBoard("Films", owner = null)
        val accounts = FakeAccountRepository(initial = ann)
        val preferences = FakeAppPreferences()
        val ownership = BoardOwnership(accounts, database.ownerDao(), preferences)

        engine(accounts = accounts, preferences = preferences, ownership = ownership).sync()
        assertTrue(boards.kept.isEmpty())

        ownership.release(moveGuestBoardsIn = true)
        engine(accounts = accounts, preferences = preferences, ownership = ownership).sync()

        assertEquals(listOf(uid), boards.kept.map { it.first })
        assertEquals("ann", dao.boardByUid(uid)!!.ownerUid)
    }

    @Test
    fun boardsKeptOnThisPhoneOnly_stayOutOfTheAccount() = runBlocking {
        val uid = givenLocalBoard("Films", owner = null)
        val accounts = FakeAccountRepository(initial = ann)
        val preferences = FakeAppPreferences()
        val ownership = BoardOwnership(accounts, database.ownerDao(), preferences)

        ownership.release(moveGuestBoardsIn = false)
        engine(accounts = accounts, preferences = preferences, ownership = ownership).sync()

        assertTrue(boards.kept.isEmpty())
        assertNull(dao.boardByUid(uid)!!.ownerUid)
    }

    // What goes up depends on the answer about this phone's own boards.
    @Test
    fun nothingGoesUp_whileThatQuestionIsOpen() = runBlocking {
        givenLocalBoard("Films", owner = "ann")
        val accounts = FakeAccountRepository(initial = ann)
        val preferences = FakeAppPreferences()
        val ownership = BoardOwnership(accounts, database.ownerDao(), preferences)
        ownership.hold()

        val report = engine(accounts = accounts, preferences = preferences, ownership = ownership).sync()

        assertFalse(report.signedIn)
        assertTrue(boards.kept.isEmpty())
    }

    @Test
    fun signingBackIn_showsThatAccountsBoards_andOnlyThose() = runBlocking {
        givenLocalBoard("Ann's films", owner = "ann")
        givenLocalBoard("Bob's games", owner = "bob")
        givenLocalBoard("Made without an account", owner = null)
        val accounts = FakeAccountRepository(initial = ann)
        val repository = RoomTierRepository(
            database.tierDao(),
            images,
            BoardOwnership(accounts, database.ownerDao(), FakeAppPreferences()),
        )
        suspend fun titles() = repository.getAllTierLists().map { it.title }

        assertEquals(listOf("Ann's films"), titles())
        accounts.accounts.value = Account.Guest
        assertEquals(listOf("Made without an account"), titles())
        accounts.accounts.value = bob
        assertEquals(listOf("Bob's games"), titles())
        accounts.accounts.value = ann
        assertEquals(listOf("Ann's films"), titles())
    }

    // Bob's account holds a board whose uid is Ann's board here: it comes down
    // to Bob as a copy, and Ann's stays exactly as it was.
    @Test
    fun anotherAccountsBoardWithTheSameUid_comesDownAsACopy() = runBlocking {
        val uid = givenLocalBoard("Films", owner = "ann")
        val bobBoards = FakeBoards()
        bobBoards.server[uid] = theirBoard(uid, title = "Films, Bob's", revision = 3)

        engine(account = bob, api = bobBoards).sync()
        engine(account = bob, api = bobBoards).sync()

        assertEquals("ann", dao.boardByUid(uid)!!.ownerUid)
        assertEquals("Films", dao.boardByUid(uid)!!.title)
        val copy = dao.boardsOf("bob").single()
        assertEquals("Films, Bob's", copy.title)
        assertEquals(true, bobBoards.server.getValue(uid).deleted)
        assertEquals(listOf(copy.uid), bobBoards.kept.map { it.first })
        assertEquals(listOf(copy.uid), dao.syncRecordsOf("bob").map { it.listUid })
    }

    // Boards from before owners: each goes to the account that agreed on it,
    // or, with none, to whoever is signed in at the first run.
    @Test
    fun boardsFromBeforeOwners_goToTheAccountThatHadThem() = runBlocking {
        val anns = givenLocalBoard("Ann's films", owner = TierListEntity.UNSETTLED)
        val unsynced = givenLocalBoard("Never synced", owner = TierListEntity.UNSETTLED)
        dao.remember(BoardSyncEntity("ann", anns, revision = 2, fingerprint = "f", syncedAt = 1))
        val bobBoards = FakeBoards()

        engine(account = bob, api = bobBoards).sync()

        assertEquals("ann", dao.boardByUid(anns)!!.ownerUid)
        assertEquals("bob", dao.boardByUid(unsynced)!!.ownerUid)
        assertEquals(listOf(unsynced), bobBoards.kept.map { it.first })
    }

    // Ann's boards once went into Bob's account too. Settled as Bob's, Ann's
    // account keeps them: they come down to her as copies, never taken out.
    @Test
    fun aBoardTwoAccountsHad_isNotTakenOutOfTheOneThatLostIt() = runBlocking {
        val uid = givenLocalBoard("Films", owner = TierListEntity.UNSETTLED)
        dao.remember(BoardSyncEntity("ann", uid, revision = 2, fingerprint = "f", syncedAt = 9))
        dao.remember(BoardSyncEntity("bob", uid, revision = 1, fingerprint = "f", syncedAt = 1))
        val annBoards = FakeBoards().apply { server[uid] = theirBoard(uid, title = "Films", revision = 2) }
        val bobBoards = FakeBoards().apply { server[uid] = theirBoard(uid, title = "Films", revision = 1) }

        engine(account = bob, api = bobBoards).sync()
        engine(account = ann, api = annBoards).sync()
        engine(account = ann, api = annBoards).sync()

        assertEquals("bob", dao.boardByUid(uid)!!.ownerUid)
        assertEquals(false, bobBoards.server.getValue(uid).deleted)
        assertEquals(listOf("Films"), dao.boardsOf("ann").map { it.title })
        assertEquals(1, annBoards.server.values.count { !it.deleted })
    }

    // Bob's account let go of a board this phone never agreed on with Bob: a
    // marker he holds says nothing about the board here.
    @Test
    fun aMarkerInAnotherAccount_neverTakesTheBoardAway() = runBlocking {
        val uid = givenLocalBoard("Films", owner = "ann")
        val annBoards = FakeBoards()
        engine(account = ann, api = annBoards).sync()
        val bobBoards = FakeBoards()
        bobBoards.server[uid] = theirBoard(uid, title = "Films", revision = 2).copy(deleted = true)

        engine(account = bob, api = bobBoards).sync()
        engine(account = ann, api = annBoards).sync()

        assertNull(dao.boardByUid(uid)!!.deletedAt)
        assertEquals(false, annBoards.server[uid]?.deleted)
    }

    // The owner's library emptied this way: off left a marker for every board,
    // and on again read each one as a delete.
    @Test
    fun backupOffAndOnAgain_keepsEveryBoard_andSendsThemBackUp() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        val games = givenLocalBoard("Games", owner = "ann")
        val preferences = FakeAppPreferences()
        val accounts = FakeAccountRepository(initial = ann)
        engine(preferences = preferences, accounts = accounts).sync()
        val backup = RoomBoardBackup(preferences, dao, images, boards, accounts, lock)

        assertTrue(backup.stopAndDelete())
        assertEquals(listOf(true, true), listOf(films, games).map { boards.server.getValue(it).deleted })
        backup.start()
        engine(preferences = preferences, accounts = accounts).sync()

        listOf(films, games).forEach { uid ->
            assertNull("$uid still in use here", dao.boardByUid(uid)!!.deletedAt)
            assertEquals("$uid back in the account", false, boards.server.getValue(uid).deleted)
            assertEquals(1, dao.syncRecordsOf("ann").count { it.listUid == uid })
        }
        assertEquals(listOf(2, 2), boards.kept.drop(2).map { it.second.basedOn })
    }

    @Test
    fun aBoardThrownAwayElsewhere_goesToTheTrashHere_andTheRecordMovesOnToTheMarker() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.server[uid] = boards.server.getValue(uid).copy(revision = 2, deleted = true)

        engine().sync()

        assertNotNull(dao.boardByUid(uid)!!.deletedAt)
        assertEquals(2, dao.allSyncRecords().single { it.listUid == uid }.revision)
    }

    // Only the person empties a trash: a delete made elsewhere never took a
    // board out of this phone's for good.
    @Test
    fun aBoardAlreadyInTheTrash_staysThere_whenThrownAwayElsewhere() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        database.tierDao().markTierListsDeleted(listOf(dao.boardByUid(uid)!!.id), deletedAt = 1)
        engine().sync()
        boards.server[uid] = boards.server.getValue(uid).let { it.copy(revision = it.revision + 1, deleted = true) }

        engine().sync()
        engine().sync()

        assertEquals(1L, dao.boardByUid(uid)!!.deletedAt)
        assertEquals(true, boards.server.getValue(uid).deleted)
    }

    @Test
    fun aSignInMidRun_stopsTheRestOfThePlan() = runBlocking {
        givenLocalBoard("Films", owner = "ann")
        givenLocalBoard("Games", owner = "ann")
        val accounts = FakeAccountRepository(initial = ann)
        val annBoards = FakeBoards().apply { onKeep = { accounts.accounts.value = bob } }

        val report = engine(api = annBoards, accounts = accounts).sync()

        assertEquals(1, annBoards.kept.size)
        assertEquals(1, report.carried)
        assertEquals(1, report.refused)
    }

    @Test
    fun rowsFromBeforeAccounts_areTakenForBoardsStillHere_andDroppedForBoardsGone() = runBlocking {
        val uid = givenLocalBoard("Films", owner = "ann")
        val annBoards = FakeBoards()
        annBoards.server[uid] = theirBoard(uid, title = "Films", revision = 4, fingerprint = fingerprintOf(uid))
        annBoards.server["gone"] = theirBoard("gone", title = "Games", revision = 2)
        dao.remember(BoardSyncEntity(BoardSyncEntity.UNCLAIMED, uid, revision = 4, fingerprint = fingerprintOf(uid), syncedAt = 0))
        dao.remember(BoardSyncEntity(BoardSyncEntity.UNCLAIMED, "gone", revision = 2, fingerprint = "theirs", syncedAt = 0))

        engine(account = ann, api = annBoards).sync()

        assertEquals("not read as a delete", false, annBoards.server["gone"]?.deleted)
        assertEquals(1, dao.boardsOf("ann").count { it.uid == uid && it.deletedAt == null })
        assertTrue(dao.syncRecordsOf(BoardSyncEntity.UNCLAIMED).isEmpty())
        assertEquals(setOf(uid, "gone"), dao.syncRecordsOf("ann").map { it.listUid }.toSet())
    }
}
