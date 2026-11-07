package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.core.settings.takeWhole
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.OwnerDao
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Longer than the server takes to erase an account; a mark this old on a live account is an erase that never happened. */
internal const val ERASE_ANSWERS_WITHIN_MS = 60L * 60 * 1000

@Singleton
class RoomBoardMerge internal constructor(
    private val dao: BoardSyncDao,
    private val owners: OwnerDao,
    private val api: BoardsApi,
    private val ownership: BoardOwnership,
    private val lock: SyncLock,
    private val preferences: AppPreferences,
    private val accounts: AccountRepository,
    private val thePerson: ThePerson,
    private val clock: () -> Long,
) : BoardMerge {

    @Inject constructor(
        dao: BoardSyncDao,
        owners: OwnerDao,
        api: BoardsApi,
        ownership: BoardOwnership,
        lock: SyncLock,
        preferences: AppPreferences,
        accounts: AccountRepository,
        thePerson: ThePerson,
    ) : this(dao, owners, api, ownership, lock, preferences, accounts, thePerson, System::currentTimeMillis)

    override fun hold() = ownership.hold()

    /** Not boards held for an account: the one signed into has them back first, and no other is asked about them. */
    override suspend fun guestBoards(): Int {
        ownership.current()?.let { ownership.reclaim(it) }
        return ownership.guestBoardsInUse().size
    }

    /** Under the lock, as holding a lost session's boards is: neither may land between the other's read and write. */
    override suspend fun moveIn(fromThisPhone: String?) = lock.withLock {
        if (fromThisPhone != null) renameClashes(fromThisPhone, ownership.guestBoardsInUse())
        ownership.release(moveGuestBoardsIn = true)
    }

    override suspend fun keepHere() = ownership.release(moveGuestBoardsIn = false)

    override suspend fun heldBoards(): Int = ownership.heldForOthersInUse().size

    /** Copies under new uids: the boards themselves stay held for their account, records and all. */
    override suspend fun copyHeldIn(fromThisPhone: String?) = lock.withLock {
        val owner = ownership.current() ?: return@withLock
        val boards = ownership.heldForOthersInUse()
        if (boards.isEmpty()) return@withLock
        val theirs = if (fromThisPhone != null) accountTitles() else emptySet()
        val uids = boards.map { it.uid }
        if (!ownership.markCopied(owner, uids)) {
            Timber.w("Could not write down what was copied; nothing is")
            return@withLock
        }
        var copied = false
        try {
            dao.together {
                boards.forEach { board ->
                    val title = if (fromThisPhone != null && board.title in theirs) withSuffix(board.title, fromThisPhone) else board.title
                    copyFor(owner, board, title)
                }
            }
            copied = true
        } finally {
            // Nothing was copied: marked so, they would never be offered again.
            if (!copied) withContext(NonCancellable) { ownership.unmarkCopied(owner, uids) }
        }
        Timber.i("Copied %d board(s) of an account signed out of into this one", boards.size)
        ownership.announceMove()
    }

    /** As a board made here: nothing published, nothing ranked, and every uid its own. */
    private suspend fun copyFor(owner: String, board: TierListEntity, title: String) {
        val allItems = dao.itemsOf(board.id)
        val allTiers = dao.tiersOf(board.id)
        val fresh = FreshUids(allTiers.map { it.uid }, allItems.map { it.uid })
        val items = allItems.groupBy { it.tierId }
        val tiers = allTiers.map { tier ->
            IncomingTier(
                tier = tier.copy(id = 0, tierListId = 0, uid = fresh.tiers.getValue(tier.uid)),
                items = items[tier.id].orEmpty().map { it.copy(id = 0, tierId = 0, uid = fresh.items.getValue(it.uid)) },
            )
        }
        val copy = board.copy(
            id = 0,
            uid = UUID.randomUUID().toString(),
            title = title,
            ownerUid = owner,
            publishedId = null,
            publishedFingerprint = null,
            rankingOf = null,
            play = fresh.play(board.play),
        )
        dao.addBoard(copy, tiers)
    }

    override suspend fun eraseKeepingBoards(
        account: String,
        erase: suspend () -> Result<Unit>,
        signOut: suspend () -> Unit,
    ): Result<Unit> = lock.ahead {
        preferences.setErasureUnderWay(ErasureMark(account, clock()).encoded())
        val result = erase()
        when {
            result.isSuccess || accounts.currentAccountExists() == false -> {
                keepBoardsOf(account)
                preferences.setLastSignedIn(null)
                signOut()
                Result.success(Unit)
            }
            result.exceptionOrNull().isRefusal() -> {
                preferences.setErasureUnderWay(null)
                result
            }
            else -> {
                Timber.w("No answer to the erase; settled on a later start")
                result
            }
        }
    }

    override suspend fun signOutSettlingErase(signOut: suspend () -> Unit) = lock.ahead {
        val mark = erasureMarkOf(preferences.erasureUnderWay())
        if (mark != null && signedInAs(accounts.current()) == mark.account) {
            if (accounts.currentAccountExists() == false) {
                keepBoardsOf(mark.account)
            } else {
                // Signed out, it can never be asked about again; an account still there keeps its boards.
                Timber.w("Signing out with an erase unanswered; the boards stay the account's")
                preferences.setErasureUnderWay(null)
            }
        }
        preferences.setLastSignedIn(null)
        signOut()
    }

    override suspend fun settleUnansweredErase() = lock.withLock {
        val mark = erasureMarkOf(preferences.erasureUnderWay()) ?: return@withLock
        val now = accounts.account.first { it != Account.Unknown }
        if (signedInAs(now) != mark.account) {
            // Signing out by choice settles the mark first, so only the account
            // going away can have signed it out. Unless the watcher saw it go,
            // what is on the phone is still that account's; otherwise it is whoever came after.
            val stillItsOwn = preferences.lastSignedIn() == mark.account
            keepBoardsOf(mark.account)
            if (stillItsOwn) thePerson.forget()
            return@withLock
        }
        when (accounts.currentAccountExists()) {
            false -> {
                keepBoardsOf(mark.account)
                preferences.setLastSignedIn(null)
                accounts.signOut()
                thePerson.forget()
            }
            true -> if (clock() - mark.atMs > ERASE_ANSWERS_WITHIN_MS) preferences.setErasureUnderWay(null)
            null -> Timber.i("Whether the account was erased cannot be told yet")
        }
    }

    private suspend fun keepBoardsOf(account: String) {
        // Held ones too: whatever it left on this phone is the phone's own now, and no longer waits for it.
        ownership.reclaim(account)
        ownership.forgetCopiesInto(account)
        val kept = owners.keepBoardsOfDeleted(account)
        preferences.setErasureUnderWay(null)
        if (preferences.lastSignedIn() == account) preferences.setLastSignedIn(null)
        Timber.i("Kept %d board(s) of the deleted account on this phone", kept)
        ownership.announceMove()
    }

    /**
     * Nothing is sent from here: the boards have uids the account has never
     * seen, so the next run creates them beside what is there. Only the two
     * sets have to be made tellable apart first.
     */
    private suspend fun renameClashes(fromThisPhone: String, boards: List<TierListEntity>) {
        val theirs = accountTitles()
        boards
            .filter { board -> board.title in theirs }
            .forEach { board -> dao.renameBoard(board.uid, withSuffix(board.title, fromThisPhone)) }
    }

    /** None when they cannot be read: both sets are still kept, only the renaming that tells them apart is lost. */
    private suspend fun accountTitles(): Set<String> = try {
        val titles = mutableSetOf<String>()
        var after: String? = null
        do {
            val page = api.index(after)
            page.boards.filterNot { it.deleted }.mapTo(titles) { it.title }
            after = page.next
        } while (after != null)
        titles
    } catch (e: IOException) {
        Timber.w(e, "Could not read the account's titles; boards keep their names")
        emptySet()
    } catch (e: HttpException) {
        Timber.w(e, "The account would not give its titles; boards keep their names")
        emptySet()
    }
}

private fun signedInAs(account: Account): String? = (account as? Account.SignedIn)?.recordKey

/** A 4xx is the server's answer that nothing was done; a 5xx may have come from halfway through. */
private fun Throwable?.isRefusal(): Boolean =
    this is HttpException && code() in HttpURLConnection.HTTP_BAD_REQUEST until HttpURLConnection.HTTP_INTERNAL_ERROR

internal data class ErasureMark(val account: String, val atMs: Long) {
    fun encoded(): String = "$account$ERASURE_SEPARATOR$atMs"
}

internal fun erasureMarkOf(encoded: String?): ErasureMark? {
    val account = encoded?.substringBeforeLast(ERASURE_SEPARATOR, missingDelimiterValue = "") ?: return null
    val atMs = encoded.substringAfterLast(ERASURE_SEPARATOR).toLongOrNull() ?: return null
    return ErasureMark(account, atMs).takeIf { account.isNotEmpty() }
}

private const val ERASURE_SEPARATOR = "\n"

/** Cut to fit rather than past the account's limit, where the suffix telling the two apart was the part cut off. */
internal fun withSuffix(title: String, suffix: String): String {
    val room = (KeptText.TITLE - suffix.length - 1).coerceAtLeast(0)
    return "${title.takeWhole(room).trimEnd()} $suffix".takeWhole(KeptText.TITLE)
}
