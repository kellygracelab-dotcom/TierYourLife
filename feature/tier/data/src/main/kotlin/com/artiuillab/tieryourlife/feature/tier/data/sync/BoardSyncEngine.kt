package com.artiuillab.tieryourlife.feature.tier.data.sync

import android.os.Build
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.BoardConflictDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptTierDto
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.LocalBoard
import com.artiuillab.tieryourlife.feature.tier.domain.sync.RemoteBoard
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncReport
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncStep
import com.artiuillab.tieryourlife.feature.tier.domain.sync.SyncedBoard
import com.artiuillab.tieryourlife.feature.tier.domain.sync.planSync
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries out a sync run.
 *
 * The thinking is in [planSync], which is pure and tested on its own. This
 * file is the plumbing around it: read the phone, read the account, do what
 * the plan says.
 *
 * Nothing here is clever about failure. A step that throws is logged and the
 * run carries on with the next board, because one board the account will not
 * take is no reason to stop backing up the other forty. The next run tries
 * again from the same three lists, so nothing needs remembering in between.
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
) : BoardSync {

    override suspend fun sync(): SyncReport {
        if (accounts.account.first() !is Account.SignedIn || !preferences.backUpBoards()) {
            return SyncReport(signedIn = false)
        }

        val local = dao.allBoards()
        val fingerprints = local.associate { board -> board.uid to fingerprintOf(board) }
        val remote = readIndex()
        val steps = planSync(
            local = local.map { board -> LocalBoard(board.uid, fingerprints.getValue(board.uid)) },
            remote = remote.values.toList(),
            synced = dao.allSyncRecords().map { SyncedBoard(it.listUid, it.revision, it.fingerprint) },
        )

        // Before the boards, so that a board naming a picture is followed by a
        // picture that is already there. The other way round leaves a window
        // where the second phone is told about something it cannot fetch.
        runCatching { pictures.push() }
            .onFailure { failure -> Timber.w(failure, "Pictures did not go up") }

        var carried = 0
        var refused = 0
        steps.forEach { step ->
            runCatching { carry(step, fingerprints) }
                .onSuccess { carried++ }
                .onFailure { failure ->
                    refused++
                    Timber.w(failure, "Board sync: %s did not go through", step.uid)
                }
        }
        runCatching { pictures.pull(wantedPictures()) }
            .onFailure { failure -> Timber.w(failure, "Pictures did not come down") }

        // Only when the whole run got through. A partial run leaves the
        // account behind on something, and the one line this timestamp feeds
        // exists to say exactly that.
        if (refused == 0) {
            preferences.setLastSyncedAtMs(System.currentTimeMillis())
        }

        return SyncReport(signedIn = true, carried = carried, refused = refused)
    }

    /**
     * Cards whose picture is meant to be here. Read off the database rather
     * than remembered from the run that fetched the board, so a download that
     * failed last week is simply tried again today.
     */
    private suspend fun wantedPictures(): Map<String, String> = dao.allBoards()
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

    private suspend fun carry(step: SyncStep, fingerprints: Map<String, String>) {
        when (step) {
            is SyncStep.Create -> send(step.uid, basedOn = null, fingerprints = fingerprints)
            is SyncStep.Update -> send(step.uid, basedOn = step.basedOn, fingerprints = fingerprints)
            is SyncStep.KeepBoth -> {
                // Theirs comes down first and lands beside ours, so that if
                // the write below never happens their afternoon is already
                // safe on this phone.
                adopt(api.board(step.uid), asCopy = true)
                send(step.uid, basedOn = step.basedOn, fingerprints = fingerprints)
            }
            is SyncStep.Restore -> takeOver(api.board(step.uid))
            is SyncStep.Adopt -> adopt(api.board(step.uid))
            is SyncStep.DiscardLocal -> {
                dao.deleteBoardByUid(step.uid)
                dao.forget(step.uid)
            }
            is SyncStep.Forget -> {
                api.forget(step.uid)
                dao.forget(step.uid)
            }
            is SyncStep.Remember -> remember(step.uid, step.revision, fingerprints.getValue(step.uid))
        }
    }

    /**
     * A refused write is the interesting one. The account hands back what it
     * already had, that copy is kept beside this phone's own under a new uid,
     * and then this phone's version goes up against the revision the refusal
     * named -- so the slot ends up holding what is on the screen in front of
     * whoever is syncing.
     */
    private suspend fun send(uid: String, basedOn: Int?, fingerprints: Map<String, String>) {
        val board = dao.boardByUid(uid) ?: return
        val fingerprint = fingerprints[uid] ?: fingerprintOf(board)
        val answer = api.keep(uid, request(board, fingerprint, basedOn))

        if (answer.isSuccessful) {
            val revision = answer.body()?.revision ?: return
            return remember(uid, revision, fingerprint)
        }
        val theirs = conflictBoard(answer) ?: run {
            Timber.w("Board sync: the account would not take %s (%d)", uid, answer.code())
            return
        }

        adopt(theirs, asCopy = true)
        val second = api.keep(uid, request(board, fingerprint, basedOn = theirs.revision))
        if (second.isSuccessful) {
            second.body()?.let { remember(uid, it.revision, fingerprint) }
        }
    }

    private fun conflictBoard(answer: Response<*>): KeptBoardDto? {
        if (answer.code() != HTTP_CONFLICT) return null
        val body = answer.errorBody()?.string() ?: return null
        return runCatching { json.decodeFromString<BoardConflictDto>(body).board }.getOrNull()
    }

    /** The account's copy replaces what is here, uid and all. */
    private suspend fun takeOver(incoming: KeptBoardDto) {
        val existing = dao.boardByUid(incoming.uid) ?: return adopt(incoming)
        dao.replaceContents(incoming.toEntity(existing.id, existing.uid), incoming.tiers(images::pathFor))
        remember(incoming.uid, incoming.revision, fingerprintOf(dao.boardByUid(incoming.uid) ?: return))
    }

    /**
     * A board arrives that this phone does not have. As itself when it is
     * simply news from another device; as a second board with a new uid when
     * it lost a fight with the copy already here, because two boards is a
     * thing a person can sort out and a silent overwrite is not.
     */
    private suspend fun adopt(incoming: KeptBoardDto, asCopy: Boolean = false) {
        val uid = if (asCopy) UUID.randomUUID().toString() else incoming.uid
        dao.addBoard(
            board = incoming.toEntity(id = 0, uid = uid).copy(
                arrivedFrom = if (asCopy) incoming.deviceName ?: UNNAMED_DEVICE else null,
                // A copy is not the published list; two boards pointing at one
                // published id would fight over it on the next publish.
                publishedId = if (asCopy) null else incoming.publishedId,
            ),
            // A copy stands beside a board that still holds the original ids,
            // and every uid in the database is unique. Renaming the rows is
            // the price of having both.
            tiers = incoming.tiers(images::pathFor, renamed = asCopy),
        )
        if (asCopy) return

        val stored = dao.boardByUid(uid) ?: return
        remember(uid, incoming.revision, fingerprintOf(stored))
    }

    private suspend fun remember(uid: String, revision: Int, fingerprint: String) {
        dao.remember(
            BoardSyncEntity(
                listUid = uid,
                revision = revision,
                fingerprint = fingerprint,
                syncedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun fingerprintOf(board: TierListEntity): String =
        BoardFingerprint.of(board, dao.tiersOf(board.id), dao.itemsOf(board.id), images::pictureIdOf)

    private suspend fun request(
        board: TierListEntity,
        fingerprint: String,
        basedOn: Int?,
    ): KeepBoardRequestDto {
        val tiers = dao.tiersOf(board.id)
        val uidByTierId = tiers.associate { it.id to it.uid }
        return KeepBoardRequestDto(
            basedOn = basedOn,
            deviceName = Build.MODEL,
            fingerprint = fingerprint,
            title = board.title,
            displayMode = board.displayMode,
            category = board.category,
            coverImageUrl = board.coverImageUrl,
            authorName = board.authorName,
            publishedId = board.publishedId,
            deletedAt = board.deletedAt,
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
            items = dao.itemsOf(board.id).mapNotNull { item ->
                val tierUid = uidByTierId[item.tierId] ?: return@mapNotNull null
                KeptItemDto(
                    uid = item.uid,
                    pictureId = images.pictureIdOf(item.imageUrl),
                    tierUid = tierUid,
                    position = item.position,
                    title = item.title,
                    imageUrl = item.imageUrl,
                    source = item.source,
                    deletedAt = item.deletedAt,
                )
            },
        )
    }

    private companion object {
        const val HTTP_CONFLICT = 409
        const val UNNAMED_DEVICE = "another phone"
    }
}

private fun KeptBoardDto.toEntity(id: Long, uid: String) = TierListEntity(
    id = id,
    title = title,
    deletedAt = deletedAt,
    displayMode = displayMode,
    publishedId = publishedId,
    authorName = authorName,
    category = category,
    coverImageUrl = coverImageUrl,
    uid = uid,
)

/**
 * Cards are handed back to their tiers by uid. A card naming a tier that is
 * not here is dropped rather than guessed at -- the account refuses to store
 * one, so this only ever happens to a board written by something else.
 */
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
                    title = item.title,
                    // A picture of their own is written as the place it will
                    // live once it arrives. Until then the card shows its
                    // title on a plain tile, which is what a card with no
                    // picture has always looked like.
                    imageUrl = item.pictureId?.let(pathFor) ?: item.imageUrl,
                    source = item.source,
                    deletedAt = item.deletedAt,
                    uid = idFor(item.uid),
                )
            },
        )
    }
}
