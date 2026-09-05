package com.artiuillab.tieryourlife.feature.tier.domain.sync

// What a sync run should do, answered from three lists and nothing else, so
// it can be tested without a network, a database or a clock.

/** A board as it is on this phone. */
data class LocalBoard(
    val uid: String,
    /** Stands in for the whole contents: change anything and it changes. Only ever compared. */
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

    /** Changed in both places. The order is the content, so nothing merges: the account's copy arrives as a second board. */
    data class KeepBoth(override val uid: String, val basedOn: Int) : SyncStep

    /** Thrown away somewhere else. Let it go here too. */
    data class DiscardLocal(override val uid: String) : SyncStep

    /** Thrown away here. Tell the account, so the other phone lets go as well. */
    data class Forget(override val uid: String) : SyncStep

    /** Already the same on both sides. Still a step: the phone must write down that they agree, or every run asks again. */
    data class Remember(override val uid: String, val revision: Int) : SyncStep
}

/**
 * [local] is every board on the phone, trash included: emptying the trash on
 * one phone means it everywhere. [remote] keeps a marker for every board ever
 * thrown away, so a board missing from it was never sent. [synced] is the last
 * agreement; a row with no board on the phone is how a delete is noticed.
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

    // Once here, no longer: emptied out of the trash on this phone. Nothing
    // else makes this shape.
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

    // Asked first: a database restored from a system backup has every board
    // twice with no record of agreeing, and the safe reading would duplicate
    // the lot.
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
        // Neither side moved and the fingerprints disagree: written before
        // phones sent one. Nothing to do until an edit.
        else -> null
    }
}
