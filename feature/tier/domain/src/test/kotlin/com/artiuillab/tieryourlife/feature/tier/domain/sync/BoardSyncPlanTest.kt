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

    // Seen on the site: an echo of the agreed board, under a new revision,
    // turned an edit made here into a copy carrying older content.
    @Test
    fun `a re-save of the agreed board is no change there, so an edit here simply goes up`() {
        val steps = planSync(local("bbb"), remote(revision = 4, fingerprint = "aaa"), synced(revision = 3, fingerprint = "aaa"))

        assertEquals(listOf(SyncStep.Update(board, basedOn = 4)), steps)
    }

    @Test
    fun `a re-save of the agreed board with nothing changed here only takes the new number`() {
        val steps = planSync(local("aaa"), remote(revision = 4, fingerprint = "aaa"), synced(revision = 3, fingerprint = "aaa"))

        assertEquals(listOf(SyncStep.Remember(board, revision = 4)), steps)
    }

    @Test
    fun `a new revision with other content is still a change there`() {
        val steps = planSync(local("bbb"), remote(revision = 4, fingerprint = "ccc"), synced(revision = 3, fingerprint = "aaa"))

        assertEquals(listOf(SyncStep.KeepBoth(board, basedOn = 4)), steps)
    }

    @Test
    fun `a new revision with other content comes down when nothing changed here`() {
        val steps = planSync(local("aaa"), remote(revision = 4, fingerprint = "ccc"), synced(revision = 3, fingerprint = "aaa"))

        assertEquals(listOf(SyncStep.Restore(board)), steps)
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
    fun `a board thrown away in the account goes to the trash here`() {
        val steps = planSync(local(), remote(revision = 2, deleted = true), synced(revision = 1))

        assertEquals(listOf(SyncStep.TrashLocal(board, revision = 2)), steps)
    }

    // The trash step writes the marker's revision down. Forgetting the record
    // instead read the marker as never agreed on next run, and the board went
    // back up into the account the other phone had just emptied.
    @Test
    fun `a board already sent to the trash for a marker stays there`() {
        val steps = planSync(local("trashed"), remote(revision = 2, deleted = true), synced(revision = 2, fingerprint = "trashed"))

        assertEquals(emptyList<SyncStep>(), steps)
    }

    @Test
    fun `an edit here outweighs a delete made elsewhere`() {
        val steps = planSync(local("bbb"), remote(revision = 2, deleted = true), synced(revision = 1, fingerprint = "aaa"))

        assertEquals(listOf(SyncStep.Revive(board, basedOn = 2)), steps)
    }

    // A36: trashing it here changed the fingerprint, which read as an edit,
    // and a board deleted for good elsewhere came back out of this trash.
    @Test
    fun `a board in the trash here stays deleted when it was deleted for good elsewhere`() {
        val inTrash = listOf(LocalBoard(board, "trashed", inTrash = true))

        val steps = planSync(inTrash, remote(revision = 2, deleted = true), synced(revision = 1, fingerprint = "aaa"))

        assertEquals(listOf(SyncStep.TrashLocal(board, revision = 2)), steps)
    }

    @Test
    fun `a board taken back out of the trash here goes back up`() {
        val steps = planSync(local("restored"), remote(revision = 2, deleted = true), synced(revision = 2, fingerprint = "trashed"))

        assertEquals(listOf(SyncStep.Revive(board, basedOn = 2)), steps)
    }

    // What emptied the library: backup off leaves a marker for every board and
    // drops every record, and on again read each marker as a delete.
    @Test
    fun `a marker for a board this phone never agreed on is written over, not obeyed`() {
        val steps = planSync(local(), remote(revision = 5, deleted = true), synced = emptyList())

        assertEquals(listOf(SyncStep.Revive(board, basedOn = 5)), steps)
    }

    @Test
    fun `backup switched off and on again sends every board back up`() {
        val steps = planSync(
            local = listOf(LocalBoard("first", "aaa"), LocalBoard("second", "bbb"), LocalBoard("never-sent", "ccc")),
            remote = listOf(
                RemoteBoard("first", revision = 3, deleted = true),
                RemoteBoard("second", revision = 8, deleted = true),
            ),
            synced = emptyList(),
        )

        assertEquals(
            listOf(
                SyncStep.Revive("first", basedOn = 3),
                SyncStep.Revive("second", basedOn = 8),
                SyncStep.Create("never-sent"),
            ),
            steps,
        )
    }

    @Test
    fun `a marker is obeyed only by the account that agreed on the board`() {
        val agreed = planSync(local(), remote(revision = 2, deleted = true), synced(revision = 1))
        val neverAgreed = planSync(local(), remote(revision = 2, deleted = true), synced = emptyList())

        assertEquals(listOf(SyncStep.TrashLocal(board, revision = 2)), agreed)
        assertEquals(listOf(SyncStep.Revive(board, basedOn = 2)), neverAgreed)
    }

    @Test
    fun `a board already gone from both sides only loses its record`() {
        val steps = planSync(local = emptyList(), remote = remote(deleted = true), synced = synced())

        assertEquals(listOf(SyncStep.DropRecord(board)), steps)
    }

    @Test
    fun `a record for a board neither side has is left alone`() {
        val steps = planSync(local = emptyList(), remote = emptyList(), synced = synced())

        assertEquals(emptyList<SyncStep>(), steps)
    }

    @Test
    fun `a board brought down once is not brought down again`() {
        val steps = planSync(local = emptyList(), remote = remote(), synced = synced())

        assertEquals(listOf(SyncStep.Forget(board)), steps)
    }

    // The scenario the fingerprint exists for: the database comes home from a
    // system backup, and without the comparison every board doubles.
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
