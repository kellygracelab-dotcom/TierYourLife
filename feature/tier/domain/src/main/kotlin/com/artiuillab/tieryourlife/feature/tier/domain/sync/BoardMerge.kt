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
     * Boards of another account this phone was signed out of without a
     * sign-out by choice, the trash left out. They wait for that account, and
     * are never part of the question about [guestBoards].
     */
    suspend fun heldBoards(): Int

    /**
     * The person moves [heldBoards] into the signed-in account; renamed as in
     * [moveIn]. The account they waited for keeps its copies of them. Nothing
     * syncs yet: [moveIn] or [keepHere] still follows.
     */
    suspend fun moveHeldIn(fromThisPhone: String?)

    /**
     * Deletes [account] with [erase] and signs out with [signOut], no sync
     * running in between. Once it is gone, its boards on this phone, trash
     * included, become boards made without an account: the account was where
     * they were copied to, not where they lived, and its uid is never signed
     * into again. Gone also when [erase] got no answer but the account is no
     * longer there; still unknown, it is settled by [settleUnansweredErase].
     */
    suspend fun eraseKeepingBoards(account: String, erase: suspend () -> Result<Unit>, signOut: suspend () -> Unit): Result<Unit>

    /** Signing out by choice: an erase still unanswered for this account is settled first, while it can still be asked about. */
    suspend fun signOutSettlingErase(signOut: suspend () -> Unit)

    /** At start: an erase that never answered is settled once the account can be asked about, or turns out signed out. */
    suspend fun settleUnansweredErase()
}
