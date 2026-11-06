package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptTierDto
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.data.repository.RoomTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupRefusal
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/** The QA findings of 2026-11-06 on sync, each as the run that showed it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SyncFindingsTest : SyncEngineBench() {

    // A3: "Stop and delete" on one phone left a marker for every board. The
    // other phone trashed its library, forgot every record, and its next run
    // sent the lot back up into the account the first had just emptied.
    @Test
    fun stopAndDeleteElsewhere_sendsTheBoardsHereToTheTrash_andNeverBackUp() = runBlocking {
        val films = givenLocalBoard("Films")
        val games = givenLocalBoard("Games")
        engine().sync()
        listOf(films, games).forEach { uid -> boards.forget(uid) }

        engine().sync()
        boards.kept.clear()
        engine().sync()
        engine().sync()

        listOf(films, games).forEach { uid ->
            assertNotNull("$uid in the trash here", dao.boardByUid(uid)!!.deletedAt)
            assertEquals("$uid still deleted in the account", true, boards.server.getValue(uid).deleted)
        }
        assertTrue(boards.kept.isEmpty())
    }

    // A7: deleted for good on one phone, it came back on the other's second run.
    @Test
    fun aBoardDeletedForGoodElsewhere_staysDeleted() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.forget(uid)

        repeat(3) { engine().sync() }

        assertEquals(1, boards.kept.size)
        assertEquals(true, boards.server.getValue(uid).deleted)
        assertNotNull(dao.boardByUid(uid)!!.deletedAt)
    }

    @Test
    fun aBoardTakenBackOutOfTheTrash_goesBackUp() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.forget(uid)
        engine().sync()

        database.tierDao().restoreTierLists(listOf(dao.boardByUid(uid)!!.id))
        engine().sync()

        assertNull(dao.boardByUid(uid)!!.deletedAt)
        assertEquals(false, boards.server.getValue(uid).deleted)
        assertEquals(null, boards.kept.last().second.deletedAt)
    }

    @Test
    fun anEditHere_outweighsADeleteElsewhere() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.forget(uid)
        dao.renameBoard(uid, "Films, edited here")

        engine().sync()

        assertNull(dao.boardByUid(uid)!!.deletedAt)
        assertEquals("Films, edited here", boards.server.getValue(uid).title)
        assertEquals(false, boards.server.getValue(uid).deleted)
    }

    @Test
    fun aRecordForABoardGoneFromBothSides_isDropped_withoutTellingTheAccount() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.forget(uid)
        engine().sync()
        throwAway(uid, signedInAs = someone)
        val revision = boards.server.getValue(uid).revision

        engine().sync()

        assertTrue(dao.allSyncRecords().none { it.listUid == uid })
        assertEquals(revision, boards.server.getValue(uid).revision)
    }

    // W3: the account's copy came down on every run while our write kept
    // being refused, one more board each time.
    @Test
    fun aConflictWhoseWriteIsRefused_keepsOneCopy_howeverManyRuns() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.server[uid] = theirBoard(uid, title = "Films, edited on B", revision = 2, fingerprint = "theirs-2")
        dao.renameBoard(uid, "Films, edited on A")
        boards.refuseWithCode = 413

        repeat(3) { engine().sync() }

        assertEquals(listOf("Films, edited on A", "Films, edited on B"), dao.allBoards().map { it.title }.sorted())
    }

    @Test
    fun aRefusalCarryingADifferentBoard_thenRefusedForGood_keepsOneCopy() = runBlocking {
        val uid = givenLocalBoard("Films")
        boards.refuseNextWith = theirBoard(uid, title = "Films (tablet)", revision = 3, deviceName = "Tablet")
        boards.thenRefuseWithCode = 413

        repeat(3) { engine().sync() }

        assertEquals(listOf("Films", "Films (tablet)"), dao.allBoards().map { it.title }.sorted())
    }

    // A9: a marker in a refusal was taken for a board: one with no title and
    // no tiers, which the account then refused on every run.
    @Test
    fun aRefusalCarryingAMarker_makesNoEmptyBoard_andOursGoesUpOverIt() = runBlocking {
        val uid = givenLocalBoard("Films")
        boards.refuseNextWith = KeptBoardDto(uid = uid, revision = 4, deleted = true)

        val report = engine().sync()

        assertEquals(0, report.refused)
        assertEquals(listOf("Films"), dao.allBoards().map { it.title })
        assertEquals(4, boards.kept.last().second.basedOn)
    }

    @Test
    fun aBoardThatTurnsOutThrownAway_whileBeingRestored_goesToTheTrash() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.server[uid] = theirBoard(uid, title = "Films, renamed", revision = 2, fingerprint = "theirs-2")
        boards.onBoard = { boards.forget(it) }

        engine().sync()

        assertEquals(listOf("Films"), dao.allBoards().map { it.title })
        assertNotNull(dao.boardByUid(uid)!!.deletedAt)
    }

    @Test
    fun aMarkerIsNeverAdoptedAsABoard() = runBlocking {
        boards.server["remote-1"] = theirBoard("remote-1", title = "Games", revision = 4)
        boards.onBoard = { boards.forget(it) }

        engine().sync()

        assertTrue(dao.allBoards().isEmpty())
    }

    // Play's Apply holds the board's row ids; a restore that wrote every row
    // anew left it moving cards that no longer existed.
    @Test
    fun aRestore_keepsTheRowsOfTiersAndCardsStillOnTheBoard() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        val here = dao.boardByUid(uid)!!
        val tier = dao.tiersOf(here.id).single()
        val card = dao.itemsOf(here.id).single()
        boards.server[uid] = KeptBoardDto(
            uid = uid,
            revision = 2,
            fingerprint = "theirs-2",
            title = "Films",
            tiers = listOf(
                KeptTierDto(uid = tier.uid, position = 0, label = "S+", colorLight = "#111111", colorDark = "#EEEEEE"),
                KeptTierDto(uid = "new-tier", position = 1, label = "A", colorLight = "#222222", colorDark = "#DDDDDD"),
            ),
            items = listOf(
                KeptItemDto(uid = card.uid, tierUid = "new-tier", position = 0, title = "Alien, renamed"),
                KeptItemDto(uid = "new-card", tierUid = tier.uid, position = 0, title = "Heat"),
            ),
        )

        engine().sync()

        val tiers = dao.tiersOf(here.id)
        val cards = dao.itemsOf(here.id)
        assertEquals("S+", tiers.single { it.id == tier.id }.label)
        val moved = cards.single { it.id == card.id }
        assertEquals("Alien, renamed", moved.title)
        assertEquals(tiers.single { it.uid == "new-tier" }.id, moved.tierId)
        assertEquals(listOf("Alien, renamed", "Heat"), cards.map { it.title }.sorted())
    }

    @Test
    fun aRestore_dropsTheTiersAndCardsNoLongerOnTheBoard() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.server[uid] = theirBoard(uid, title = "Films", revision = 2, fingerprint = "theirs-2")

        engine().sync()

        val here = dao.boardByUid(uid)!!
        assertEquals(listOf("$uid-tier"), dao.tiersOf(here.id).map { it.uid })
        assertEquals(listOf("Doom"), dao.itemsOf(here.id).map { it.title })
    }

    // A8: a card added while the run read the index was wiped by the
    // restore the run had planned before it.
    @Test
    fun anEditMadeWhileARestoreIsUnderWay_isKept() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        boards.server[uid] = theirBoard(uid, title = "Films, renamed on B", revision = 2, fingerprint = "theirs-2")
        boards.onIndex = {
            boards.onIndex = {}
            dao.renameBoard(uid, "Films, edited during the run")
        }

        engine().sync()
        engine().sync()

        assertEquals(
            listOf("Films, edited during the run", "Films, renamed on B"),
            dao.allBoards().map { it.title }.sorted(),
        )
        assertEquals("Films, edited during the run", boards.server.getValue(uid).title)
    }

    // A10: switched off mid-run, the run went on to send both boards back up
    // after the deletes, and nothing on this phone would ever remove them.
    @Test
    fun stopAndDeleteDuringARun_leavesNothingInTheAccount() = runBlocking {
        givenLocalBoard("Films", owner = "ann")
        val preferences = FakeAppPreferences()
        val accounts = FakeAccountRepository(initial = ann)
        engine(preferences = preferences, accounts = accounts).sync()
        givenLocalBoard("Games", owner = "ann")
        val backup = RoomBoardBackup(preferences, dao, images, boards, accounts, lock)
        var stopped: Deferred<Boolean>? = null
        boards.onIndex = {
            boards.onIndex = {}
            stopped = async { backup.stopAndDelete() }
            yield()
        }

        engine(preferences = preferences, accounts = accounts).sync()

        assertEquals(true, stopped?.await())
        assertTrue(boards.server.values.all { it.deleted })
        assertFalse(preferences.backUpBoards("ann"))
        assertTrue(dao.syncRecordsOf("ann").isEmpty())
    }

    @Test
    fun stopAndDelete_takesOutBoardsThisPhoneNeverBroughtDown() = runBlocking {
        val preferences = FakeAppPreferences()
        val accounts = FakeAccountRepository(initial = ann)
        boards.server["from-the-site"] = theirBoard("from-the-site", title = "Games", revision = 1)

        assertTrue(RoomBoardBackup(preferences, dao, images, boards, accounts, lock).stopAndDelete())

        assertEquals(true, boards.server.getValue("from-the-site").deleted)
    }

    // A11: the account collapses spaces and cuts at 80; the phone that typed
    // the text got the cut version back on the next pull.
    @Test
    fun textGoesUpAsTheAccountKeepsIt_andIsKeptSoHereToo() = runBlocking {
        val long = "A".repeat(100)
        val uid = givenLocalBoard("Films  of   the year", itemTitle = "Blade  Runner $long")

        engine().sync()

        val sent = boards.kept.single().second
        assertEquals("Films of the year", sent.title)
        assertEquals(80, sent.items.single().title.length)
        assertTrue(sent.items.single().title.startsWith("Blade Runner A"))
        val here = dao.boardByUid(uid)!!
        assertEquals("Films of the year", here.title)
        assertEquals(sent.items.single().title, dao.itemsOf(here.id).single().title)
        assertEquals(fingerprintOf(uid), sent.fingerprint)
    }

    // A2: two photos added at once make cards with no name, which /boards
    // refuses, and the whole board never backed up again.
    @Test
    fun aCardWithoutAName_goesUpUnderAStandIn_andComesDownNameless() = runBlocking {
        givenLocalBoard("Holiday", itemTitle = "")
        boards.server["remote-1"] = theirBoard("remote-1", title = "Trip", revision = 1, pictureId = "beach.jpg").let { board ->
            board.copy(items = board.items.map { it.copy(title = KeptText.UNTITLED_CARD) })
        }

        engine().sync()

        assertEquals(listOf(KeptText.UNTITLED_CARD), boards.kept.single().second.items.map { it.title })
        val arrived = dao.boardByUid("remote-1")!!
        assertEquals(listOf(""), dao.itemsOf(arrived.id).map { it.title })
    }

    // W5: refused for what the board is, it is named in Settings at once
    // rather than blamed on the connection a day later.
    @Test
    fun aBoardTheAccountTurnsAway_isWrittenDown_untilARunGetsThrough() = runBlocking {
        givenLocalBoard("Films")
        val preferences = FakeAppPreferences()
        boards.refuseWithCode = 413

        engine(preferences = preferences).sync()
        assertEquals(
            BackupRefusal("Films", BackupRefusal.Reason.TooLarge),
            backupRefusalOf(preferences.boardRefusal(someone)),
        )

        boards.refuseWithCode = null
        engine(preferences = preferences).sync()
        assertNull(preferences.boardRefusal(someone))
    }

    @Test
    fun anAccountWithNoRoom_isToldApartFromAConflict() = runBlocking {
        givenLocalBoard("Films")
        val preferences = FakeAppPreferences()
        boards.refuseWithCode = 409
        boards.refusalBody = """{"error":"An account keeps up to 200 boards","code":"TOO_MANY_BOARDS"}"""

        engine(preferences = preferences).sync()

        assertEquals(BackupRefusal.Reason.TooManyBoards, backupRefusalOf(preferences.boardRefusal(someone))?.reason)
        assertEquals(1, dao.allBoards().size)
    }

    @Test
    fun aRunThatOnlyLostTheConnection_keepsWhatWasRefusedBefore() = runBlocking {
        givenLocalBoard("Films")
        val preferences = FakeAppPreferences()
        boards.refuseWithCode = 400
        engine(preferences = preferences).sync()

        boards.refuseWithCode = null
        boards.keepFails = true
        engine(preferences = preferences).sync()

        assertEquals(BackupRefusal.Reason.NotAccepted, backupRefusalOf(preferences.boardRefusal(someone))?.reason)
    }

    // A1: the boards of a deleted account sat under a uid nobody signs into again.
    @Test
    fun deletingTheAccount_keepsItsBoardsHere_asBoardsWithoutAnAccount() = runBlocking {
        val films = givenLocalBoard("Films", publishedId = "pub-1", owner = "ann")
        val others = givenLocalBoard("Bob's games", owner = "bob")
        engine(account = ann).sync()
        val accounts = FakeAccountRepository(initial = ann)
        val preferences = FakeAppPreferences()
        val ownership = BoardOwnership(accounts, database.ownerDao(), preferences)
        val merge = RoomBoardMerge(dao, database.ownerDao(), boards, ownership, lock)

        val result = merge.eraseKeepingBoards("ann") { Result.success(Unit) }

        assertTrue(result.isSuccess)
        assertNull(dao.boardByUid(films)!!.ownerUid)
        assertNull(dao.boardByUid(films)!!.publishedId)
        assertEquals("bob", dao.boardByUid(others)!!.ownerUid)
        assertTrue(dao.syncRecordsOf("ann").isEmpty())
        accounts.accounts.value = Account.Guest
        val repository = RoomTierRepository(database.tierDao(), images, ownership)
        assertEquals(listOf("Films"), repository.getAllTierLists().map { it.title })
    }

    @Test
    fun anAccountThatWasNotDeleted_keepsItsBoards() = runBlocking {
        val films = givenLocalBoard("Films", owner = "ann")
        val accounts = FakeAccountRepository(initial = ann)
        val ownership = BoardOwnership(accounts, database.ownerDao(), FakeAppPreferences())
        val merge = RoomBoardMerge(dao, database.ownerDao(), boards, ownership, lock)

        val result = merge.eraseKeepingBoards("ann") { Result.failure(IOException("offline")) }

        assertTrue(result.isFailure)
        assertEquals("ann", dao.boardByUid(films)!!.ownerUid)
    }
}
