package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupRefusal
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** QA's second pass over sync, 2026-11-06. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SyncSecondPassTest : SyncEngineBench() {

    // A36: moving it to the trash here changed its fingerprint, which read as
    // an edit, so a board deleted for good on the other phone came back.
    @Test
    fun aBoardInTheTrashHere_staysDeleted_whenDeletedForGoodElsewhere() = runBlocking {
        val uid = givenLocalBoard("Films")
        engine().sync()
        database.tierDao().markTierListsDeleted(listOf(dao.boardByUid(uid)!!.id), deletedAt = 1)
        boards.forget(uid)
        boards.kept.clear()

        engine().sync()
        engine().sync()

        assertNotNull(dao.boardByUid(uid)!!.deletedAt)
        assertEquals(true, boards.server.getValue(uid).deleted)
        assertTrue(boards.kept.isEmpty())
    }

    // W5: an undo can take a board past the tiers the account keeps; "take
    // some cards off it" sent people after the wrong thing.
    @Test
    fun aBoardWithTooManyTiers_isRefusedForItsTiers() = runBlocking {
        givenLocalBoard("Films")
        val preferences = FakeAppPreferences()
        boards.refuseWithCode = 413
        boards.refusalBody = """{"error":"Too many tiers","code":"TOO_LARGE"}"""

        engine(preferences = preferences).sync()

        assertEquals(BackupRefusal.Reason.TooManyTiers, backupRefusalOf(preferences.boardRefusal(someone))?.reason)
    }

    @Test
    fun aBoardOverTheTiers_isToldSoEvenWhenTheAnswerDoesNotSay() = runBlocking {
        val uid = givenLocalBoard("Films")
        val board = dao.boardByUid(uid)!!.id
        repeat(TierList.MAX_RANKED_TIERS + 1) { index ->
            database.tierDao().insertTier(
                TierEntity(tierListId = board, position = index + 1, label = "T$index", colorLight = "#111111", colorDark = "#EEEEEE"),
            )
        }
        val preferences = FakeAppPreferences()
        boards.refuseWithCode = 413

        engine(preferences = preferences).sync()

        assertEquals(BackupRefusal.Reason.TooManyTiers, backupRefusalOf(preferences.boardRefusal(someone))?.reason)
    }

    @Test
    fun aBoardTooLargeForItsCards_isStillToldSo() = runBlocking {
        givenLocalBoard("Films")
        val preferences = FakeAppPreferences()
        boards.refuseWithCode = 413
        boards.refusalBody = """{"error":"Too many items","code":"TOO_LARGE"}"""

        engine(preferences = preferences).sync()

        assertEquals(BackupRefusal.Reason.TooLarge, backupRefusalOf(preferences.boardRefusal(someone))?.reason)
    }

    // QA 484 N2: an edit waits for the next run only as long as the phone can tell it is one.
    @Test
    fun whatIsUnsent_isEachChangedBoard_andNothingOnceTheAccountAgrees() = runBlocking {
        val uid = givenLocalBoard("Films")
        val engine = engine()
        assertEquals(setOf("$uid:${fingerprintOf(uid)}"), engine.unsent())

        engine.sync()
        assertEquals(emptySet<String>(), engine.unsent())

        database.tierDao().updateTierListTitle(dao.boardByUid(uid)!!.id, "Films I love")
        assertEquals(setOf("$uid:${fingerprintOf(uid)}"), engine.unsent())
        assertEquals(emptySet<String>(), engine(backUp = false).unsent())
    }

    // A12: the list screen hears of a run nobody on it started.
    @Test
    fun everyRun_isAnnouncedAsItEnds() = runBlocking {
        givenLocalBoard("Films")
        val engine = engine()
        val heard = async(start = CoroutineStart.UNDISPATCHED) { engine.finished.first() }

        val report = engine.sync()

        assertEquals(report, heard.await())
    }
}
