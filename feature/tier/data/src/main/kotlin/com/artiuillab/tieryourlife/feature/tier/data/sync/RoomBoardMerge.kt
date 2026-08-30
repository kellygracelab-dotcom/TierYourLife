package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import com.artiuillab.tieryourlife.feature.tier.domain.sync.MergeChoice
import timber.log.Timber
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
    override suspend fun choice(): MergeChoice {
        val accountBoards = runCatching { api.index().boards.count { !it.deleted } }
            .onFailure { failure -> Timber.w(failure, "Could not read what the account holds") }
            .getOrDefault(0)
        return MergeChoice(accountBoards = accountBoards, localBoards = dao.boardsInUse().size)
    }

    /**
     * Nothing is moved and nothing is sent. The boards on this phone have uids
     * the account has never seen, so the next run simply creates them beside
     * what is already there; the only thing that has to happen first is making
     * the two sets tellable apart.
     */
    override suspend fun keepEverything(fromThisPhone: String) {
        val theirs = runCatching { api.index().boards.filterNot { it.deleted }.map { it.title } }
            .getOrElse { return }
            .toSet()

        dao.boardsInUse()
            .filter { board -> board.title in theirs }
            .forEach { board -> dao.renameBoard(board.uid, "${board.title} $fromThisPhone") }
    }

    override suspend fun useAccountBoards() {
        dao.trashEveryBoard(System.currentTimeMillis())
    }
}
