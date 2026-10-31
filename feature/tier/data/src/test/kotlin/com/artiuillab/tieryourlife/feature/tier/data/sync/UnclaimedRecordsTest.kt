package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.domain.sync.RemoteBoard
import org.junit.Assert.assertEquals
import org.junit.Test

class UnclaimedRecordsTest {

    @Test
    fun aRowTheAccountAgreesWith_isTaken() {
        val row = row("films", revision = 3, fingerprint = "f")

        val taken = claimable(listOf(row), mapOf("films" to "f"), mapOf("films" to remote("films", 3, "f")))

        assertEquals(listOf(row), taken)
    }

    // Read as a delete, this row could take a board out of an account that never threw it away.
    @Test
    fun aRowWhoseBoardIsGone_isNotTaken() {
        val row = row("films", revision = 3, fingerprint = "f")

        val taken = claimable(listOf(row), emptyMap(), mapOf("films" to remote("films", 3, "f")))

        assertEquals(emptyList<BoardSyncEntity>(), taken)
    }

    @Test
    fun aRowThatWouldSendAnEditOverADifferentBoardAtThatRevision_isNotTaken() {
        val row = row("films", revision = 3, fingerprint = "someone else's")

        val taken = claimable(listOf(row), mapOf("films" to "edited here"), mapOf("films" to remote("films", 3, "theirs")))

        assertEquals(emptyList<BoardSyncEntity>(), taken)
    }

    // Nothing is sent from it: the board here is still what the row says.
    @Test
    fun aRowForABoardUnchangedHere_isTakenEvenIfTheAccountDisagrees() {
        val row = row("films", revision = 3, fingerprint = "f")

        val taken = claimable(listOf(row), mapOf("films" to "f"), mapOf("films" to remote("films", 3, "theirs")))

        assertEquals(listOf(row), taken)
    }

    @Test
    fun aRowTheAccountHasMovedOnFrom_isTaken() {
        val row = row("films", revision = 3, fingerprint = "f")

        val taken = claimable(listOf(row), mapOf("films" to "edited here"), mapOf("films" to remote("films", 5, "theirs")))

        assertEquals(listOf(row), taken)
    }

    // Written before phones sent a fingerprint: nothing to contradict the row, and dropping it would copy every old board.
    @Test
    fun aRowForABoardWithNoFingerprintInTheAccount_isTaken() {
        val row = row("films", revision = 3, fingerprint = "f")

        val taken = claimable(listOf(row), mapOf("films" to "edited here"), mapOf("films" to remote("films", 3, null)))

        assertEquals(listOf(row), taken)
    }

    private fun row(uid: String, revision: Int, fingerprint: String) = BoardSyncEntity(
        account = BoardSyncEntity.UNCLAIMED,
        listUid = uid,
        revision = revision,
        fingerprint = fingerprint,
        syncedAt = 0,
    )

    private fun remote(uid: String, revision: Int, fingerprint: String?) =
        RemoteBoard(uid = uid, revision = revision, deleted = false, fingerprint = fingerprint)
}
