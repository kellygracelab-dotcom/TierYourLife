package com.artiuillab.tieryourlife.feature.tier.domain.sync

/**
 * What to do when somebody signs into an account that already has boards on
 * it.
 *
 * Only ever a question in one case, and it is worth being exact about which:
 * an account with nothing on it takes this phone's boards silently, because
 * there is nothing to weigh them against. The question is for the person who
 * built something without an account and then signed into one they had used
 * before -- two sets of work, neither of them disposable.
 */
data class MergeChoice(val accountBoards: Int, val localBoards: Int) {

    /** Nothing to ask when only one side has anything. */
    val needed: Boolean get() = accountBoards > 0 && localBoards > 0

    val total: Int get() = accountBoards + localBoards
}

interface BoardMerge {

    suspend fun choice(): MergeChoice

    /**
     * Both sets survive. Boards from this phone that share a name with one in
     * the account are renamed, because two boards called "Sci-fi films" with
     * different insides is the state somebody cannot act on.
     */
    suspend fun keepEverything(fromThisPhone: String)

    /**
     * The account's boards are the ones in use. This phone's go to the trash,
     * where they can be taken back for thirty days -- "nothing is deleted
     * either way" has to be true, or the choice is a trap.
     */
    suspend fun useAccountBoards()
}
