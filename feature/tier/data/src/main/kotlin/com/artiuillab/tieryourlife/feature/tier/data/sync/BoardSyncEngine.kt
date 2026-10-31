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
) : BoardSync {

    override suspend fun sync(): SyncReport {
        val account = signedInAs()
        if (account.isNullOrBlank() || !preferences.backUpBoards()) {
            return SyncReport(signedIn = false)
        }

        val local = dao.allBoards()
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
        runSuspendCatching { pictures.push() }
            .onFailure { failure -> Timber.w(failure, "Pictures did not go up") }

        var carried = 0
        var refused = 0
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
                    Timber.w(failure, "Board sync: %s did not go through", step.uid)
                }
        }
        runSuspendCatching { pictures.pull(wantedPictures()) }
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
        val claimed = dao.allBoards().mapNotNullTo(mutableSetOf()) { it.publishedId }
        val theirs = if (claimed.isEmpty()) {
            emptySet()
        } else {
            community.myPublished().getOrElse { return }.mapTo(mutableSetOf()) { it.id }
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
            dao.forgetPublished(reconciled.forget.toList())
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
        val unclaimed = dao.syncRecordsOf(BoardSyncEntity.UNCLAIMED)
        if (unclaimed.isEmpty()) return
        val claimed = claimable(unclaimed, fingerprints, remote)
        dao.claim(claimed.map { it.copy(account = account) })
        Timber.i("Claimed %d of %d unclaimed sync record(s)", claimed.size, unclaimed.size)
    }

    /** Read from the database each run, so a download that failed last week is tried again. */
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

    private suspend fun carry(step: SyncStep, fingerprints: Map<String, String>, account: String) {
        when (step) {
            is SyncStep.Create -> send(account, step.uid, basedOn = null, fingerprints = fingerprints)
            is SyncStep.Update -> send(account, step.uid, basedOn = step.basedOn, fingerprints = fingerprints)
            is SyncStep.KeepBoth -> {
                // Theirs comes down first, so it is safe here even if the
                // write below never happens.
                adopt(account, api.board(step.uid), asCopy = true)
                send(account, step.uid, basedOn = step.basedOn, fingerprints = fingerprints)
            }
            is SyncStep.Restore -> takeOver(account, api.board(step.uid))
            is SyncStep.Adopt -> adopt(account, api.board(step.uid))
            is SyncStep.DiscardLocal -> {
                dao.deleteBoardByUid(step.uid)
                dao.forgetEverywhere(step.uid)
            }
            is SyncStep.Forget -> {
                api.forget(step.uid)
                dao.forget(account, step.uid)
            }
            is SyncStep.Remember -> remember(account, step.uid, step.revision, fingerprints.getValue(step.uid))
        }
    }

    /**
     * On a refused write the account's copy is kept beside ours under a new
     * uid, then ours goes up against the revision the refusal named.
     */
    private suspend fun send(account: String, uid: String, basedOn: Int?, fingerprints: Map<String, String>) {
        val board = dao.boardByUid(uid) ?: return
        val fingerprint = fingerprints[uid] ?: fingerprintOf(board)
        val answer = api.keep(uid, request(board, fingerprint, basedOn))

        if (answer.isSuccessful) {
            val revision = answer.body()?.revision ?: error("The account kept $uid but named no revision")
            return remember(account, uid, revision, fingerprint)
        }
        // Thrown rather than logged and returned: the run counts it as refused,
        // so the last-synced time stays put and the stuck banner can show.
        val theirs = conflictBoard(answer) ?: throw HttpException(answer)

        // Our own board coming back: the last write's answer never arrived.
        // Same content, so take the missed number and stop -- otherwise a
        // kill at the wrong moment leaves a copy of your own board every time.
        if (theirs.fingerprint != null && theirs.fingerprint == fingerprint) {
            return remember(account, uid, theirs.revision, fingerprint)
        }

        adopt(account, theirs, asCopy = true)
        val second = api.keep(uid, request(board, fingerprint, basedOn = theirs.revision))
        if (!second.isSuccessful) throw HttpException(second)
        second.body()?.let { remember(account, uid, it.revision, fingerprint) }
    }

    private fun conflictBoard(answer: Response<*>): KeptBoardDto? {
        if (answer.code() != HTTP_CONFLICT) return null
        val body = answer.errorBody()?.string() ?: return null
        return runSuspendCatching { json.decodeFromString<BoardConflictDto>(body).board }.getOrNull()
    }

    /** The account's copy replaces what is here, uid and all. */
    private suspend fun takeOver(account: String, incoming: KeptBoardDto) {
        val existing = dao.boardByUid(incoming.uid) ?: return adopt(account, incoming)
        dao.replaceContents(incoming.toEntity(existing.id, existing.uid).keepingLocalFieldsOf(existing), incoming.tiers(images::pathFor))
        remember(account, incoming.uid, incoming.revision, fingerprintOf(dao.boardByUid(incoming.uid) ?: return))
    }

    /**
     * As itself when it is news from another device; as a copy with a new uid
     * when it lost to the board already here -- two boards a person can sort
     * out, a silent overwrite they cannot.
     */
    private suspend fun adopt(account: String, incoming: KeptBoardDto, asCopy: Boolean = false) {
        val uid = if (asCopy) UUID.randomUUID().toString() else incoming.uid
        dao.addBoard(
            board = incoming.toEntity(id = 0, uid = uid).copy(
                // Null rather than a placeholder: the wording belongs to the screen.
                arrivedFrom = if (asCopy) incoming.deviceName else null,
                // A copy is not the published list; two boards pointing at one
                // published id would fight over it on the next publish.
                publishedId = if (asCopy) null else incoming.publishedId,
            ),
            // Renamed: every uid is unique and the original board still holds them.
            tiers = incoming.tiers(images::pathFor, renamed = asCopy),
        )
        if (asCopy) return

        val stored = dao.boardByUid(uid) ?: return
        remember(account, uid, incoming.revision, fingerprintOf(stored))
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

    private suspend fun request(
        board: TierListEntity,
        fingerprint: String,
        basedOn: Int?,
    ): KeepBoardRequestDto {
        val tiers = dao.tiersOf(board.id)
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
    }
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
                    title = item.title,
                    // Written as the path it will live at once fetched; until
                    // then the tile shows the title, like any card without one.
                    imageUrl = item.pictureId?.let(pathFor) ?: item.imageUrl,
                    source = item.source,
                    deletedAt = item.deletedAt,
                    uid = idFor(item.uid),
                )
            },
        )
    }
}
