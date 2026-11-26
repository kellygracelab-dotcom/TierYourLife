package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardDto
import timber.log.Timber

/**
 * For [BoardSyncEngine]: what each board looked like when this phone and the
 * account last agreed on it, and the merge that makes possible when a board
 * changed on both sides since.
 */
internal class Agreements(
    private val dao: BoardSyncDao,
    private val images: TierImageStore,
    private val store: LastAgreedBoards,
) {
    private val merger = ThreeWayMerge { images.pictureIdOf(it) }

    /**
     * A board unchanged since the last agreement is that agreement: kept now,
     * so a board agreed on before this phone kept them can be merged too.
     */
    suspend fun seed(account: String, boards: List<TierListEntity>, fingerprints: Map<String, String>, records: List<BoardSyncEntity>) {
        val agreedOn = records.associate { it.listUid to it.fingerprint }
        boards.filter { agreedOn[it.uid] == fingerprints[it.uid] && !store.holds(account, it.uid) }.forEach { board ->
            store.write(account, board.uid, merger.agreedOf(fingerprints.getValue(board.uid), sideOf(board)))
        }
    }

    /** Written with each record of agreeing; [side] when what was agreed is not the board here as it stands. */
    suspend fun agreed(account: String, uid: String, fingerprint: String, side: BoardSide?) {
        store.answered(account, uid)
        val agreedOn = side ?: dao.boardByUid(uid)?.let { sideOf(it) }?.takeIf { it.fingerprint() == fingerprint }
        if (agreedOn == null) store.forget(account, uid) else store.write(account, uid, merger.agreedOf(fingerprint, agreedOn))
    }

    fun keepOnly(account: String, uids: Collection<String>) = store.keepOnly(account, uids)

    fun sending(account: String, uid: String, fingerprint: String) = store.sending(account, uid, fingerprint)

    /**
     * The account's board is one this phone sent since they last agreed, though
     * the answer never came: our own write, not another phone's, so nothing to
     * merge or keep beside.
     */
    fun isOurOwnLastWrite(account: String, theirs: KeptBoardDto): Boolean =
        !theirs.deleted && theirs.fingerprint != null && theirs.fingerprint in store.sentSinceAgreed(account, theirs.uid)

    /**
     * Changed here and in the account since the last agreement: the account's
     * changes are taken into the board here, as long as no field of one thing
     * changed on both sides. False leaves the board as it was, for a second one.
     * [settle] writes the text as the account keeps it first, as a write would.
     */
    suspend fun merge(account: String, theirs: KeptBoardDto, settle: suspend (TierListEntity) -> Unit): Boolean {
        if (theirs.deleted) return false
        val record = dao.syncRecordOf(account, theirs.uid) ?: return false
        val base = store.read(account, theirs.uid)?.takeIf { it.fingerprint == record.fingerprint } ?: return false
        dao.boardByUid(theirs.uid)?.let { settle(it) }
        var merged = false
        dao.together {
            val here = dao.boardByUid(theirs.uid) ?: return@together
            val result = merger.merge(base, sideOf(here), sideOf(theirs)) ?: return@together
            val star = laterStar(here.star, theirs.star)
            dao.replaceContents(result.board.copy(favouritedAt = star.at, favouriteChangedAt = star.changedAt), result.tiers)
            merged = true
        }
        Timber.i(if (merged) "A board changed on both sides is merged" else "A board changed on both sides keeps both")
        return merged
    }

    fun sideOf(theirs: KeptBoardDto): BoardSide {
        val tiers = theirs.tiers(images::pathFor)
        return BoardSide(theirs.toEntity(id = 0, uid = theirs.uid, pathFor = images::pathFor), tiers.map { it.tier }, tiers.flatMap { it.items })
    }

    private suspend fun sideOf(board: TierListEntity) = BoardSide(board, dao.tiersOf(board.id), dao.itemsOf(board.id))

    private fun BoardSide.fingerprint() = BoardFingerprint.of(board, tiers, items, images::pictureIdOf)
}
