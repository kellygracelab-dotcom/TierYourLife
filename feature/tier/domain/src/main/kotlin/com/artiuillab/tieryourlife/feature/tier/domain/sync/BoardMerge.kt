package com.artiuillab.tieryourlife.feature.tier.domain.sync

/**
 * What to do when somebody signs into an account that already has boards.
 * Only a question when both sides hold something: two sets of work, neither
 * disposable.
 */
data class MergeChoice(val accountBoards: Int, val localBoards: Int) {

    /** Nothing to ask when only one side has anything. */
    val needed: Boolean get() = accountBoards > 0 && localBoards > 0

    val total: Int get() = accountBoards + localBoards
}

interface BoardMerge {

    suspend fun choice(): MergeChoice

    /** Both survive; boards from this phone sharing a name with one in the account are renamed. */
    suspend fun keepEverything(fromThisPhone: String)

    /** The account's boards are the ones in use; this phone's go to the trash, so "nothing is deleted either way" stays true. */
    suspend fun useAccountBoards()
}
