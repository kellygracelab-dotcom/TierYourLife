package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import com.artiuillab.tieryourlife.feature.tier.domain.sync.MergeChoice
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBoardMerge @Inject constructor(
    private val dao: BoardSyncDao,
    private val api: BoardsApi,
) : BoardMerge {

    /**
     * Counted from the account's index rather than from anything remembered
     * here: this phone has just signed into an identity it may never have seen,
     * and what that identity holds is only knowable by asking.
     */
    override suspend fun choice(): MergeChoice? {
        val accountBoards = try {
            api.index().boards.count { !it.deleted }
        } catch (e: IOException) {
            Timber.w(e, "Could not read what the account holds")
            return null
        } catch (e: HttpException) {
            Timber.w(e, "The account would not say what it holds")
            return null
        }
        return MergeChoice(accountBoards = accountBoards, localBoards = dao.boardsInUse().size)
    }

    /**
     * Nothing is moved or sent: the boards here have uids the account has never
     * seen, so the next run creates them beside what is there. Only the two
     * sets have to be made tellable apart first.
     */
    override suspend fun keepEverything(fromThisPhone: String) {
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

        dao.boardsInUse()
            .filter { board -> board.title in theirs }
            .forEach { board -> dao.renameBoard(board.uid, "${board.title} $fromThisPhone") }
    }

    override suspend fun useAccountBoards() {
        dao.trashEveryBoard(System.currentTimeMillis())
    }
}
