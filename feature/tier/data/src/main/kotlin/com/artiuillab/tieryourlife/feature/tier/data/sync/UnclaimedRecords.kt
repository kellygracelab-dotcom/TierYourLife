package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.domain.sync.RemoteBoard

/**
 * Which rows from before they named an account the first account to sync may
 * take as its own: every one, unless something says it is not. A row whose
 * board is gone is not taken, since read as a delete it would go into an
 * account that may never have asked; at worst that board comes back. Nor is
 * one that would send this phone's edit over a different board at that same
 * revision; the two arrive side by side instead.
 */
internal fun claimable(
    unclaimed: List<BoardSyncEntity>,
    fingerprints: Map<String, String>,
    remote: Map<String, RemoteBoard>,
): List<BoardSyncEntity> = unclaimed.filter { record ->
    val here = fingerprints[record.listUid] ?: return@filter false
    val theirs = remote[record.listUid]
    val otherBoardAtThatRevision = theirs != null && theirs.revision == record.revision &&
        theirs.fingerprint != null && theirs.fingerprint != record.fingerprint
    !(otherBoardAtThatRevision && here != record.fingerprint)
}
