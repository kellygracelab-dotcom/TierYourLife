package com.artiuillab.tieryourlife.feature.tier.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardSyncPlanTest {

    private val board = "board-1"

    private fun local(fingerprint: String = "aaa") = listOf(LocalBoard(board, fingerprint))

    private fun remote(
        revision: Int = 1,
        deleted: Boolean = false,
        fingerprint: String? = null,
    ) = listOf(RemoteBoard(board, revision, deleted, fingerprint))

    private fun synced(revision: Int = 1, fingerprint: String = "aaa") =
        listOf(SyncedBoard(board, revision, fingerprint))

    @Test
    fun `a board the account has never seen is sent`() {
        val steps = planSync(local(), remote = emptyList(), synced = emptyList())

        assertEquals(listOf(SyncStep.Create(board)), steps)
    }

    @Test
    fun `a board changed only here is sent over what the account has`() {
        val steps = planSync(local("bbb"), remote(revision = 3), synced(revision = 3))

        assertEquals(listOf(SyncStep.Update(board, basedOn = 3)), steps)
    }

    @Test
    fun `a board changed only in the account comes back down`() {
        val steps = planSync(local(), remote(revision = 4), synced(revision = 3))

        assertEquals(listOf(SyncStep.Restore(board)), steps)
    }

    @Test
    fun `a board nobody touched costs nothing`() {
        val steps = planSync(local(), remote(revision = 3), synced(revision = 3))

        assertEquals(emptyList<SyncStep>(), steps)
    }

    // The order of the cards is the content, so there is no merge. Both
    // afternoons are kept and the person decides.
    @Test
    fun `a board changed in both places is kept twice`() {
        val steps = planSync(local("bbb"), remote(revision = 4), synced(revision = 3))

        assertEquals(listOf(SyncStep.KeepBoth(board, basedOn = 4)), steps)
    }

    @Test
    fun `a board on the account and not on the phone is brought down`() {
        val steps = planSync(local = emptyList(), remote = remote(), synced = emptyList())

        assertEquals(listOf(SyncStep.Adopt(board)), steps)
    }

    // Emptying the trash on one phone has to mean it everywhere, or it is not
    // a delete, it is a delete on the phone you happened to use.
    @Test
    fun `a board thrown away here is reported to the account`() {
        val steps = planSync(local = emptyList(), remote = remote(), synced = synced())

        assertEquals(listOf(SyncStep.Forget(board)), steps)
    }

    @Test
    fun `a board thrown away in the account goes here too`() {
        val steps = planSync(local(), remote(deleted = true), synced())

        assertEquals(listOf(SyncStep.DiscardLocal(board)), steps)
    }

    @Test
    fun `a board already gone from both sides is left alone`() {
        val steps = planSync(local = emptyList(), remote = remote(deleted = true), synced = synced())

        assertEquals(emptyList<SyncStep>(), steps)
    }

    @Test
    fun `a board brought down once is not brought down again`() {
        val steps = planSync(local = emptyList(), remote = remote(), synced = synced())

        assertEquals(listOf(SyncStep.Forget(board)), steps)
    }

    // The scenario this whole fingerprint exists for: the database comes home
    // from a system backup, so both sides hold the board and nothing records
    // that they ever agreed. Without the comparison every board doubles.
    @Test
    fun `a database back from a system backup does not duplicate anything`() {
        val steps = planSync(local("aaa"), remote(revision = 2, fingerprint = "aaa"), synced = emptyList())

        assertEquals(listOf(SyncStep.Remember(board, revision = 2)), steps)
    }

    @Test
    fun `a database back from a system backup keeps both when it really differs`() {
        val steps = planSync(local("bbb"), remote(revision = 2, fingerprint = "aaa"), synced = emptyList())

        assertEquals(listOf(SyncStep.KeepBoth(board, basedOn = 2)), steps)
    }

    // Two phones that ended up at the same arrangement did not have a fight,
    // however many revisions apart their counters are.
    @Test
    fun `two sides that hold the same board only write down that they agree`() {
        val steps = planSync(local("aaa"), remote(revision = 9, fingerprint = "aaa"), synced(revision = 3))

        assertEquals(listOf(SyncStep.Remember(board, revision = 9)), steps)
    }

    @Test
    fun `a board the account kept before fingerprints existed is left alone`() {
        val steps = planSync(local("aaa"), remote(revision = 3, fingerprint = null), synced(revision = 3))

        assertEquals(emptyList<SyncStep>(), steps)
    }

    @Test
    fun `every board gets its own step`() {
        val steps = planSync(
            local = listOf(LocalBoard("mine", "aaa"), LocalBoard("edited", "ccc")),
            remote = listOf(
                RemoteBoard("edited", revision = 1, deleted = false),
                RemoteBoard("theirs", revision = 1, deleted = false),
            ),
            synced = listOf(SyncedBoard("edited", revision = 1, fingerprint = "bbb")),
        )

        assertEquals(
            listOf(
                SyncStep.Create("mine"),
                SyncStep.Update("edited", basedOn = 1),
                SyncStep.Adopt("theirs"),
            ),
            steps,
        )
    }
}
