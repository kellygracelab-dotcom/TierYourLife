package com.artiuillab.tieryourlife.feature.tier.domain.sync

/**
 * What becomes of the boards made without an account when somebody signs in.
 * A first sign-in, which turns the guest into the account, takes them without
 * a word; signing into an account used before asks, since they may not be that
 * account's to have. Another account's boards are never part of the question.
 */
interface BoardMerge {

    /** Before signing in: nothing syncs from then until [moveIn] or [keepHere], so nothing goes up before the question is settled. */
    fun hold()

    /** Boards made without an account, the trash left out. */
    suspend fun guestBoards(): Int

    /**
     * They become the signed-in account's and go up with its next sync. Those
     * sharing a name with one already in it get [fromThisPhone] added; null
     * for an account too new to hold any.
     */
    suspend fun moveIn(fromThisPhone: String?)

    /** They stay this phone's own, shown whenever nobody is signed in. */
    suspend fun keepHere()

    /**
     * Deletes [account] with [erase], no sync running meanwhile. Once it is
     * gone, its boards on this phone, trash included, become boards made
     * without an account: the account was where they were copied to, not
     * where they lived, and its uid is never signed into again.
     */
    suspend fun eraseKeepingBoards(account: String, erase: suspend () -> Result<Unit>): Result<Unit>
}
