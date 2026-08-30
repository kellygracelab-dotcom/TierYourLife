package com.artiuillab.tieryourlife.feature.tier.domain.sync

// What a sync run should do, worked out before anything is sent or written.
//
// Pure on purpose. Every hard question here -- which side is ahead, whether two
// devices touched the same board, whether a board that is missing was thrown
// away or never sent -- is answered from three lists and nothing else, so the
// answers can be tested without a network, a database or a clock.

/** A board as it is on this phone. */
data class LocalBoard(
    val uid: String,
    /**
     * A short stand-in for the whole board's contents. Same contents, same
     * fingerprint; change anything and it changes. Only ever compared.
     */
    val fingerprint: String,
)

/** A board as the account has it, from the index. */
data class RemoteBoard(
    val uid: String,
    val revision: Int,
    /** True once the board was thrown away; nothing else is left of it. */
    val deleted: Boolean,
    /** The fingerprint of whichever phone wrote it last, if it sent one. */
    val fingerprint: String? = null,
)

/** What this phone knew last time it and the account agreed about a board. */
data class SyncedBoard(
    val uid: String,
    val revision: Int,
    val fingerprint: String,
)

sealed interface SyncStep {

    val uid: String

    /** The account has never seen this board. Send it. */
    data class Create(override val uid: String) : SyncStep

    /** Changed here and nowhere else. Send it over what the account has. */
    data class Update(override val uid: String, val basedOn: Int) : SyncStep

    /** Changed in the account and not here. Take theirs. */
    data class Restore(override val uid: String) : SyncStep

    /** A board this phone has never had. Bring it down. */
    data class Adopt(override val uid: String) : SyncStep

    /**
     * Changed in both places. There is no arithmetic that merges two
     * arrangements of the same cards -- the order is the content -- so the
     * account's copy arrives as a second board and this one keeps the slot.
     */
    data class KeepBoth(override val uid: String, val basedOn: Int) : SyncStep

    /** Thrown away somewhere else. Let it go here too. */
    data class DiscardLocal(override val uid: String) : SyncStep

    /** Thrown away here. Tell the account, so the other phone lets go as well. */
    data class Forget(override val uid: String) : SyncStep

    /**
     * Nothing to send and nothing to fetch: the two already hold the same
     * board. Worth a step of its own because the phone still has to write down
     * that they agree, and until it does, every later run asks the same
     * question again.
     */
    data class Remember(override val uid: String, val revision: Int) : SyncStep
}

/**
 * Works out the whole run.
 *
 * [local] is every board on the phone, trashed ones included -- a board in the
 * trash is still a board, and someone who empties the trash on one phone means
 * it everywhere. [remote] is the account's index, which keeps a marker for
 * every board ever thrown away, so a board missing from it has genuinely never
 * been sent. [synced] is what this phone recorded the last time the two agreed;
 * a row here with no board on the phone is how a delete is noticed at all.
 */
fun planSync(
    local: List<LocalBoard>,
    remote: List<RemoteBoard>,
    synced: List<SyncedBoard>,
): List<SyncStep> {
    val remoteByUid = remote.associateBy { it.uid }
    val syncedByUid = synced.associateBy { it.uid }
    val localUids = local.mapTo(mutableSetOf()) { it.uid }
    val steps = mutableListOf<SyncStep>()

    local.forEach { board ->
        stepFor(board, remoteByUid[board.uid], syncedByUid[board.uid])?.let { steps += it }
    }

    remote.forEach { board ->
        if (board.uid !in localUids && !board.deleted && board.uid !in syncedByUid) {
            steps += SyncStep.Adopt(board.uid)
        }
    }

    // A board this phone once had and no longer does was emptied out of the
    // trash here. Nothing else makes this shape: a board it never had has no
    // row, and a board it still has was handled above.
    synced.forEach { record ->
        val stillHere = record.uid in localUids
        val alreadyGone = remoteByUid[record.uid]?.deleted ?: true
        if (!stillHere && !alreadyGone) {
            steps += SyncStep.Forget(record.uid)
        }
    }

    return steps
}

private fun stepFor(local: LocalBoard, remote: RemoteBoard?, synced: SyncedBoard?): SyncStep? {
    if (remote == null) {
        return SyncStep.Create(local.uid)
    }
    if (remote.deleted) {
        // Thrown away on another phone. Keeping it here would make emptying the
        // trash something that only holds on the phone you did it on.
        return SyncStep.DiscardLocal(local.uid)
    }

    // Asked before anything else, because it is the one question a revision
    // number cannot answer. A database that came home from a system backup has
    // every board twice with no record of the two ever agreeing, and without
    // this the safe reading is "two afternoons" -- which duplicates the lot.
    if (remote.fingerprint != null && remote.fingerprint == local.fingerprint) {
        return SyncStep.Remember(local.uid, remote.revision)
    }
    if (synced == null) {
        return SyncStep.KeepBoth(local.uid, basedOn = remote.revision)
    }

    val changedHere = local.fingerprint != synced.fingerprint
    val changedThere = remote.revision != synced.revision

    return when {
        changedHere && changedThere -> SyncStep.KeepBoth(local.uid, basedOn = remote.revision)
        changedHere -> SyncStep.Update(local.uid, basedOn = remote.revision)
        changedThere -> SyncStep.Restore(local.uid)
        // Neither side moved, and the fingerprints still disagree: the account
        // holds a board written before phones sent one. Nothing to do about it
        // until somebody edits something.
        else -> null
    }
}
