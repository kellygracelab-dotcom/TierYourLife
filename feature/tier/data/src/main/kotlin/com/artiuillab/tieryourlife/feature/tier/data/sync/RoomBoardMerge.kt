package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.OwnerDao
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.data.repository.BoardOwnership
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBoardMerge @Inject constructor(
    private val dao: BoardSyncDao,
    private val owners: OwnerDao,
    private val api: BoardsApi,
    private val ownership: BoardOwnership,
    private val lock: SyncLock,
) : BoardMerge {

    override fun hold() = ownership.hold()

    override suspend fun guestBoards(): Int = owners.guestBoardsInUse().size

    override suspend fun moveIn(fromThisPhone: String?) {
        if (fromThisPhone != null) renameClashes(fromThisPhone)
        ownership.release(moveGuestBoardsIn = true)
    }

    override suspend fun keepHere() = ownership.release(moveGuestBoardsIn = false)

    override suspend fun eraseKeepingBoards(account: String, erase: suspend () -> Result<Unit>): Result<Unit> = lock.withLock {
        erase().onSuccess {
            val kept = owners.keepBoardsOfDeleted(account)
            Timber.i("Kept %d board(s) of the deleted account on this phone", kept)
            ownership.announceMove()
        }
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

        owners.guestBoardsInUse()
            .filter { board -> board.title in theirs }
            .forEach { board -> dao.renameBoard(board.uid, withSuffix(board.title, fromThisPhone)) }
    }
}

/** Cut to fit rather than past the account's limit, where the suffix telling the two apart was the part cut off. */
internal fun withSuffix(title: String, suffix: String): String {
    val room = (KeptText.TITLE - suffix.length - 1).coerceAtLeast(0)
    return "${title.take(room).trimEnd()} $suffix".take(KeptText.TITLE)
}
