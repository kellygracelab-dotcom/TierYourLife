package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toCoverImageUrls
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toStoredCoverImageUrls
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.BoardConflictDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptTierDto
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.LocalBoard
import com.artiuillab.tieryourlife.feature.tier.domain.sync.RemoteBoard
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncReport
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncStep
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncedBoard
import com.artiuillab.tieryourlife.feature.tier.domain.sync.planSync
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries out a sync run; the thinking is in [planSync], which is pure. A step
 * that throws is logged and the run carries on with the next board -- the next
 * run recomputes from the same lists, so nothing is remembered in between.
 */
@Singleton
class BoardSyncEngine @Inject constructor(
    private val dao: BoardSyncDao,
    private val api: BoardsApi,
    private val accounts: AccountRepository,
    private val json: Json,
    private val images: TierImageStore,
    private val pictures: PictureSync,
    private val preferences: AppPreferences,
    private val deviceName: DeviceName,
    private val community: CommunityRepository,
    private val ownership: BoardOwnership,
    private val lock: SyncLock,
) : BoardSync {

    /**
     * One run at a time. The list screen and a returning connection both start
     * one, at launch together; a run that read the boards before the other
     * restored one took the restore for an edit here and sent it back up.
     */
    override suspend fun sync(): SyncReport = lock.withLock { run() }

    /**
     * Only the signed-in account's own boards: another account's, or the ones
     * made without one, are never sent, whoever is signed in. Nothing while a
     * sign-in waits on the question of what becomes of the latter.
     */
    private suspend fun run(): SyncReport {
        val account = ownership.current()
        if (account.isNullOrBlank() || ownership.holding || !preferences.backUpBoards(account)) {
            return SyncReport(signedIn = false)
        }

        val local = dao.boardsOf(account)
        val fingerprints = local.associate { board -> board.uid to fingerprintOf(board) }
        val remote = readIndex()
        claimUnclaimed(account, fingerprints, remote)
        val steps = planSync(
            local = local.map { board -> LocalBoard(board.uid, fingerprints.getValue(board.uid)) },
            remote = remote.values.toList(),
            synced = dao.syncRecordsOf(account).map { SyncedBoard(it.listUid, it.revision, it.fingerprint) },
        )

        // Pictures before boards, so no board ever names a picture the other
        // phone cannot fetch yet.
        runSuspendCatching { pictures.push(account) }
            .onFailure { failure -> Timber.w(failure, "Pictures did not go up") }

        var carried = 0
        var refused = 0
        var refusal: BackupRefusal? = null
        for ((index, step) in steps.withIndex()) {
            // Planned from this account's rows; another one signed in since would be handed them.
            if (signedInAs() != account) {
                refused += steps.size - index
                break
            }
            runSuspendCatching { carry(step, fingerprints, account) }
                .onSuccess { carried++ }
                .onFailure { failure ->
                    refused++
                    if (failure is RefusedByAccount) refusal = refusal ?: failure.refusal
                    Timber.w(failure, "Board sync: %s did not go through", step.uid)
                }
        }
        // A run that failed only on the connection says nothing about a board refused before.
        val refusedBoard = refusal
        when {
            refusedBoard != null -> preferences.setBoardRefusal(account, refusedBoard.encoded())
            refused == 0 -> preferences.setBoardRefusal(account, null)
        }
        runSuspendCatching { pictures.pull(account, wantedPictures(account)) }
            .onFailure { failure -> Timber.w(failure, "Pictures did not come down") }

        runSuspendCatching { forgetWhatIsNoLongerPublished() }
            .onFailure { failure -> Timber.w(failure, "Could not reconcile what is published") }

        // Only after a complete run: a partial one leaves the account behind,
        // which is what this timestamp is there to report.
        if (refused == 0) {
            preferences.setLastSyncedAtMs(System.currentTimeMillis())
        }

        return SyncReport(signedIn = true, carried = carried, refused = refused)
    }

    /**
     * Drops a published id the account no longer has: a snapshot can vanish
     * without this phone (taken down, or unpublished elsewhere), and a board
     * still claiming to be public hides the one switch that would fix it. Only
     * the account that published an id can drop it: signing into another one
     * must not wipe the first account's lists off its boards.
     */
    private suspend fun forgetWhatIsNoLongerPublished() {
        val account = signedInAs() ?: return
        val boards = dao.boardsOf(account)
        val claimed = boards.mapNotNullTo(mutableSetOf()) { it.publishedId }
        // A board was ever published only with a section chosen.
        val unlinked = boards
            .filter { it.publishedId == null && it.rankingOf == null && it.deletedAt == null && it.category != null }
            .mapTo(mutableSetOf()) { it.uid }
        val mine = if (claimed.isEmpty() && unlinked.isEmpty()) {
            emptyMap()
        } else {
            community.myPublished().getOrElse { return }.associate { it.id to it.boardUid }
        }
        val theirs = mine.keys
        relinkPublished(unlinked, mine, claimed).forEach { (uid, publishedId) ->
            Timber.i("A board that lost its published list has it back")
            dao.relinkPublished(account, uid, publishedId)
        }
        val reconciled = reconcilePublished(
            claimed = claimed,
            inAccount = theirs,
            account = account,
            lastAccount = preferences.lastPublishingAccount(),
            owners = preferences.publishedOwners(),
        )
        if (reconciled.forget.isNotEmpty()) {
            Timber.i("%d published list(s) are no longer in the account", reconciled.forget.size)
            dao.forgetPublished(account, reconciled.forget.toList())
        }
        preferences.setPublishedOwners(reconciled.owners)
        preferences.setLastPublishingAccount(account)
    }

    private suspend fun signedInAs(): String? = (accounts.account.first() as? Account.SignedIn)?.recordKey

    private suspend fun claimUnclaimed(
        account: String,
        fingerprints: Map<String, String>,
        remote: Map<String, RemoteBoard>,
    ) {
        // Rows for another owner's boards wait for that owner's first run.
        val othersBoards = dao.allBoards().filter { it.ownerUid != account }.mapTo(mutableSetOf()) { it.uid }
        val unclaimed = dao.syncRecordsOf(BoardSyncEntity.UNCLAIMED).filterNot { it.listUid in othersBoards }
        if (unclaimed.isEmpty()) return
        val claimed = claimable(unclaimed, fingerprints, remote)
        dao.claim(considered = unclaimed.map { it.listUid }, claimed = claimed.map { it.copy(account = account) })
        Timber.i("Claimed %d of %d unclaimed sync record(s)", claimed.size, unclaimed.size)
    }

    /** Read from the database each run, so a download that failed last week is tried again. */
    private suspend fun wantedPictures(account: String): Map<String, String> = dao.boardsOf(account)
        .flatMap { board -> dao.itemsOf(board.id) }
        .mapNotNull { item ->
            val pictureId = images.pictureIdOf(item.imageUrl) ?: return@mapNotNull null
            item.uid to pictureId
        }
        .toMap()

    private suspend fun readIndex(): Map<String, RemoteBoard> {
        val found = mutableMapOf<String, RemoteBoard>()
        var after: String? = null
        do {
            val page = api.index(after)
            page.boards.forEach { summary ->
                found[summary.uid] = RemoteBoard(
                    uid = summary.uid,
                    revision = summary.revision,
                    deleted = summary.deleted,
                    fingerprint = summary.fingerprint,
                )
            }
            after = page.next
        } while (after != null)
        return found
    }

    private suspend fun carry(step: SyncStep, fingerprints: Map<String, String>, account: String) {
        when (step) {
            is SyncStep.Create -> send(account, step.uid, basedOn = null)
            is SyncStep.Update -> send(account, step.uid, basedOn = step.basedOn)
            is SyncStep.Revive -> send(account, step.uid, basedOn = step.basedOn)
            is SyncStep.KeepBoth -> {
                // Theirs comes down first, so it is safe here even if the
                // write below never happens.
                val theirs = api.board(step.uid)
                keepBeside(account, theirs)
                send(account, step.uid, basedOn = theirs.revision)
            }
            is SyncStep.Restore -> takeOver(account, api.board(step.uid), planned = fingerprints[step.uid])
            is SyncStep.Adopt -> adopt(account, api.board(step.uid))
            is SyncStep.TrashLocal -> trashHere(account, step.uid, step.revision, planned = fingerprints[step.uid])
            is SyncStep.Forget -> {
                api.forget(step.uid)
                dao.forget(account, step.uid)
            }
            is SyncStep.DropRecord -> dao.forget(account, step.uid)
            is SyncStep.Remember -> remember(account, step.uid, step.revision, fingerprints.getValue(step.uid))
        }
    }

    /**
     * Thrown away on another phone. Into the trash here and no further: only
     * the person empties it. The record moves on to the marker rather than
     * going, or the next run reads the marker as never agreed on and sends the
     * board back up. Not if it was edited since the plan: then the edit goes
     * up over the marker next run.
     */
    private suspend fun trashHere(account: String, uid: String, revision: Int, planned: String?) = dao.together {
        val board = dao.boardByUid(uid)
        if (board == null || (planned != null && fingerprintOf(board) != planned)) return@together
        if (board.deletedAt == null) dao.trashBoard(uid, System.currentTimeMillis())
        val trashed = dao.boardByUid(uid) ?: return@together
        remember(account, uid, revision, fingerprintOf(trashed))
    }

    /**
     * The account's version as a copy beside ours, and with it the record of
     * agreeing on that version: should our write then be refused for good, the
     * next run only sends ours again instead of bringing down another copy.
     * A marker is not a board, so there is nothing to keep beside.
     */
    private suspend fun keepBeside(account: String, theirs: KeptBoardDto) {
        if (theirs.deleted) return
        dao.together {
            adopt(account, theirs, asCopy = true)
            remember(account, theirs.uid, theirs.revision, theirs.fingerprint.orEmpty())
        }
    }

    /**
     * On a refused write the account's copy is kept beside ours under a new
     * uid, then ours goes up against the revision the refusal named. The
     * fingerprint is of what goes up, not of what the run started with: an
     * edit in between, recorded under the older one, went up again next run.
     */
    private suspend fun send(account: String, uid: String, basedOn: Int?) {
        val board = dao.boardByUid(uid)?.let { read ->
            settleText(read)
            dao.boardByUid(uid)
        } ?: return
        val tiers = dao.tiersOf(board.id)
        val items = dao.itemsOf(board.id)
        val fingerprint = BoardFingerprint.of(board, tiers, items, images::pictureIdOf)
        val answer = api.keep(uid, request(board, tiers, items, fingerprint, basedOn))

        if (answer.isSuccessful) {
            val revision = answer.body()?.revision ?: error("The account kept $uid but named no revision")
            return remember(account, uid, revision, fingerprint)
        }
        // Thrown rather than logged and returned: the run counts it as refused,
        // so the last-synced time stays put and the stuck banner can show.
        val refusal = refusalOf(answer)
        val theirs = refusal?.board?.takeIf { answer.code() == HTTP_CONFLICT } ?: throw refused(answer, refusal, board.title)

        // Our own board coming back: the last write's answer never arrived.
        // Same content, so take the missed number and stop -- otherwise a
        // kill at the wrong moment leaves a copy of your own board every time.
        if (!theirs.deleted && theirs.fingerprint != null && theirs.fingerprint == fingerprint) {
            return remember(account, uid, theirs.revision, fingerprint)
        }

        // Thrown away elsewhere meanwhile: ours was being sent because it
        // changed here, and that outweighs the delete.
        keepBeside(account, theirs)
        val second = api.keep(uid, request(board, tiers, items, fingerprint, basedOn = theirs.revision))
        if (!second.isSuccessful) throw refused(second, refusalOf(second), board.title)
        second.body()?.let { remember(account, uid, it.revision, fingerprint) }
    }

    private fun refusalOf(answer: Response<*>): BoardConflictDto? {
        val body = answer.errorBody()?.string() ?: return null
        return runSuspendCatching { json.decodeFromString<BoardConflictDto>(body) }.getOrNull()
    }

    private fun refused(answer: Response<*>, refusal: BoardConflictDto?, title: String): Exception {
        val reason = when {
            answer.code() == HTTP_TOO_LARGE -> BackupRefusal.Reason.TooLarge
            answer.code() == HTTP_CONFLICT && refusal?.code == TOO_MANY_BOARDS -> BackupRefusal.Reason.TooManyBoards
            answer.code() == HTTP_BAD_REQUEST -> BackupRefusal.Reason.NotAccepted
            else -> return HttpException(answer)
        }
        return RefusedByAccount(BackupRefusal(boardTitle = title, reason = reason), HttpException(answer))
    }

    /**
     * The account keeps text with whitespace collapsed and cut to its limits.
     * Written so here before it goes up, so this phone holds what every other
     * one will: otherwise the next pull rewrote what was typed here, and a lost
     * record of agreeing turned the difference into a second board.
     */
    private suspend fun settleText(board: TierListEntity) {
        KeptText.clean(board.title, KeptText.TITLE)?.takeIf { it != board.title }?.let { dao.settleBoardTitle(board.id, board.title, it) }
        dao.tiersOf(board.id).forEach { tier ->
            KeptText.clean(tier.label, KeptText.TITLE)?.takeIf { it != tier.label }?.let { dao.settleTierLabel(tier.id, tier.label, it) }
            tier.caption?.let { caption ->
                val clean = KeptText.clean(caption, KeptText.CAPTION)
                if (clean != caption) dao.settleTierCaption(tier.id, caption, clean)
            }
        }
        dao.itemsOf(board.id).forEach { item ->
            // A card without a name keeps none; it goes up under a stand-in.
            KeptText.clean(item.title, KeptText.TITLE)?.takeIf { it != item.title }?.let { dao.settleItemTitle(item.id, item.title, it) }
            item.credit?.let { credit ->
                val clean = KeptText.clean(credit, KeptText.CREDIT)
                if (clean != credit) dao.settleItemCredit(item.id, credit, clean)
            }
        }
    }

    /**
     * The account's copy replaces what is here, uid and all -- unless the
     * board was edited since the plan was made: that edit stays, and the next
     * run sees a change on both sides and keeps both. A marker in its place
     * means it was thrown away meanwhile.
     */
    private suspend fun takeOver(account: String, incoming: KeptBoardDto, planned: String?) {
        if (incoming.deleted) return trashHere(account, incoming.uid, incoming.revision, planned)
        // Gone since the plan: deleted for good here, and the next run forgets it.
        val existing = dao.boardByUid(incoming.uid) ?: return if (planned == null) adopt(account, incoming) else Unit
        dao.together {
            val now = dao.boardByUid(incoming.uid) ?: return@together
            if (planned != null && fingerprintOf(now) != planned) {
                Timber.i("A board edited during the run keeps its edit")
                return@together
            }
            dao.replaceContents(incoming.toEntity(existing.id, existing.uid).keepingLocalFieldsOf(now), incoming.tiers(images::pathFor))
            val stored = dao.boardByUid(incoming.uid) ?: return@together
            remember(account, incoming.uid, incoming.revision, fingerprintOf(stored))
        }
    }

    /**
     * As itself when it is news from another device; as a copy with a new uid
     * when it lost to the board already here -- two boards a person can sort
     * out, a silent overwrite they cannot. A marker is not a board.
     */
    private suspend fun adopt(account: String, incoming: KeptBoardDto, asCopy: Boolean = false) {
        if (incoming.deleted) return
        if (!asCopy && dao.boardByUid(incoming.uid) != null) return adoptBesideAnotherOwners(account, incoming)
        val uid = if (asCopy) UUID.randomUUID().toString() else incoming.uid
        dao.together {
            dao.addBoard(
                board = incoming.toEntity(id = 0, uid = uid).copy(
                    // Null rather than a placeholder: the wording belongs to the screen.
                    arrivedFrom = if (asCopy) incoming.deviceName else null,
                    // A copy is not the published list; two boards pointing at one
                    // published id would fight over it on the next publish.
                    publishedId = if (asCopy) null else incoming.publishedId,
                    ownerUid = account,
                ),
                // Renamed: every uid is unique and the original board still holds them.
                tiers = incoming.tiers(images::pathFor, renamed = asCopy),
            )
            if (asCopy) return@together
            val stored = dao.boardByUid(uid) ?: return@together
            remember(account, uid, incoming.revision, fingerprintOf(stored))
        }
    }

    /**
     * The uid is another owner's board here, which happens when one account's
     * boards were once sent into another. This account's copy comes down under
     * a new uid, and the record left for the old one makes the next run tell
     * the account it is gone from here; the new uid goes up in its place.
     */
    private suspend fun adoptBesideAnotherOwners(account: String, incoming: KeptBoardDto) {
        dao.addBoardRemembering(
            board = incoming.toEntity(id = 0, uid = UUID.randomUUID().toString()).copy(publishedId = null, ownerUid = account),
            tiers = incoming.tiers(images::pathFor, renamed = true),
            record = BoardSyncEntity(
                account = account,
                listUid = incoming.uid,
                revision = incoming.revision,
                fingerprint = incoming.fingerprint.orEmpty(),
                syncedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun remember(account: String, uid: String, revision: Int, fingerprint: String) {
        dao.remember(
            BoardSyncEntity(
                account = account,
                listUid = uid,
                revision = revision,
                fingerprint = fingerprint,
                syncedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun fingerprintOf(board: TierListEntity): String =
        BoardFingerprint.of(board, dao.tiersOf(board.id), dao.itemsOf(board.id), images::pictureIdOf)

    private fun request(
        board: TierListEntity,
        tiers: List<TierEntity>,
        items: List<TierItemEntity>,
        fingerprint: String,
        basedOn: Int?,
    ): KeepBoardRequestDto {
        val uidByTierId = tiers.associate { it.id to it.uid }
        return KeepBoardRequestDto(
            basedOn = basedOn,
            deviceName = deviceName.current(),
            fingerprint = fingerprint,
            title = board.title,
            displayMode = board.displayMode,
            category = board.category,
            coverImageUrl = board.coverImageUrl,
            coverImageUrls = board.coverImageUrls.toCoverImageUrls(),
            authorName = board.authorName,
            publishedId = board.publishedId,
            deletedAt = board.deletedAt,
            anonymous = true.takeIf { board.anonymous },
            play = keptPlay(board.play, json),
            tiers = tiers.map { tier ->
                KeptTierDto(
                    uid = tier.uid,
                    position = tier.position,
                    label = tier.label,
                    caption = tier.caption,
                    colorLight = tier.colorLight,
                    colorDark = tier.colorDark,
                    isPool = tier.isPool,
                )
            },
            items = items.mapNotNull { item ->
                val tierUid = uidByTierId[item.tierId] ?: return@mapNotNull null
                KeptItemDto(
                    uid = item.uid,
                    pictureId = images.pictureIdOf(item.imageUrl),
                    tierUid = tierUid,
                    position = item.position,
                    title = item.title.ifBlank { KeptText.UNTITLED_CARD },
                    imageUrl = item.imageUrl,
                    source = item.source,
                    deletedAt = item.deletedAt,
                    credit = item.credit,
                )
            },
        )
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_CONFLICT = 409
        const val HTTP_TOO_LARGE = 413
        const val TOO_MANY_BOARDS = "TOO_MANY_BOARDS"
    }
}

/** A write the account turns away for what the board is; trying again changes nothing until the board does. */
internal class RefusedByAccount(val refusal: BackupRefusal, cause: HttpException) : Exception(cause.message, cause)

private const val REFUSAL_SEPARATOR = "\n"

/** As [AppPreferences.boardRefusal] keeps it. */
internal fun BackupRefusal.encoded(): String = "${reason.name}$REFUSAL_SEPARATOR$boardTitle"

internal fun backupRefusalOf(encoded: String?): BackupRefusal? {
    val (reason, title) = encoded?.split(REFUSAL_SEPARATOR, limit = 2)?.takeIf { it.size == 2 } ?: return null
    val known = BackupRefusal.Reason.entries.firstOrNull { it.name == reason } ?: return null
    return BackupRefusal(boardTitle = title, reason = known)
}

/** The game as it came down, to go back up as it came; text that no longer reads as JSON is not worth a refused board. */
internal fun keptPlay(stored: String?, json: Json): JsonElement? = stored?.let {
    runCatching { json.parseToJsonElement(it) }
        .onFailure { failure -> Timber.w(failure, "A kept game no longer reads") }
        .getOrNull()
}

/**
 * What the account never holds stays as this phone had it: the star, the topic,
 * where a copy came from, what was last published. Without this every pulled
 * change wiped them.
 */
internal fun TierListEntity.keepingLocalFieldsOf(here: TierListEntity) = copy(
    publishedFingerprint = here.publishedFingerprint,
    topicId = here.topicId,
    topicName = here.topicName,
    newTopicName = here.newTopicName,
    takenFrom = here.takenFrom,
    rankingOf = here.rankingOf,
    arrivedFrom = here.arrivedFrom,
    favouritedAt = here.favouritedAt,
    ownerUid = here.ownerUid,
)

internal fun KeptBoardDto.toEntity(id: Long, uid: String) = TierListEntity(
    id = id,
    title = title,
    deletedAt = deletedAt,
    displayMode = displayMode,
    publishedId = publishedId,
    authorName = authorName,
    category = category,
    coverImageUrl = coverImageUrl,
    coverImageUrls = coverImageUrls.toStoredCoverImageUrls(),
    uid = uid,
    anonymous = anonymous,
    play = play?.toString(),
)

/** A card naming a tier that is not here is dropped, not guessed at; the account never stores one. */
private fun KeptBoardDto.tiers(
    pathFor: (String) -> String,
    renamed: Boolean = false,
): List<IncomingTier> {
    fun idFor(original: String) = if (renamed) UUID.randomUUID().toString() else original
    val itemsByTierUid = items.groupBy { it.tierUid }
    return tiers.sortedBy { it.position }.mapIndexed { index, tier ->
        IncomingTier(
            tier = TierEntity(
                id = index.toLong(),
                tierListId = 0,
                position = tier.position,
                label = tier.label,
                colorLight = tier.colorLight,
                colorDark = tier.colorDark,
                isPool = tier.isPool,
                caption = tier.caption,
                uid = idFor(tier.uid),
            ),
            items = itemsByTierUid[tier.uid].orEmpty().sortedBy { it.position }.map { item ->
                TierItemEntity(
                    tierId = index.toLong(),
                    position = item.position,
                    title = item.title.takeUnless { it == KeptText.UNTITLED_CARD && (item.pictureId != null || item.imageUrl != null) }.orEmpty(),
                    // Written as the path it will live at once fetched; until
                    // then the tile shows the title, like any card without one.
                    imageUrl = item.pictureId?.let(pathFor) ?: item.imageUrl,
                    source = item.source,
                    deletedAt = item.deletedAt,
                    uid = idFor(item.uid),
                    credit = item.credit,
                )
            },
        )
    }
}
