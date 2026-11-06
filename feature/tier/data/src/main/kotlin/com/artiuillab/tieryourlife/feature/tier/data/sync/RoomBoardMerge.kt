package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.OwnerDao
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
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
        if (fromThisPhone != null) renameClashes(fromThisPhone)
        ownership.release(moveGuestBoardsIn = true)
    }

    override suspend fun keepHere() = ownership.release(moveGuestBoardsIn = false)

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
            // going away can have signed it out.
            keepBoardsOf(mark.account)
            thePerson.forget()
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
    private suspend fun renameClashes(fromThisPhone: String) {
        val theirs = try {
            api.index().boards.filterNot { it.deleted }.map { it.title }.toSet()
        } catch (e: IOException) {
            // Both sets are still kept; only the renaming that tells them apart is lost.
            Timber.w(e, "Could not read the account's titles; boards keep their names")
            return
        } catch (e: HttpException) {
            Timber.w(e, "The account would not give its titles; boards keep their names")
            return
        }

        ownership.guestBoardsInUse()
            .filter { board -> board.title in theirs }
            .forEach { board -> dao.renameBoard(board.uid, withSuffix(board.title, fromThisPhone)) }
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
    return "${title.take(room).trimEnd()} $suffix".take(KeptText.TITLE)
}
