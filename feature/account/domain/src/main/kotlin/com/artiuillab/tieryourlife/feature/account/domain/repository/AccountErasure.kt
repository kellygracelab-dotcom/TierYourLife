package com.artiuillab.tieryourlife.feature.account.domain.repository

/**
 * Ending an account, as opposed to stepping away from one.
 *
 * Separate from [AccountRepository] because it is a different promise. Signing
 * out leaves everything where it is and is undone by signing back in; this
 * removes the account, the copy of the boards kept for it, the lists it
 * published and the pictures behind them, and cannot be undone by anything.
 */
interface AccountErasure {

    /**
     * Removes the account and everything the service holds about it.
     *
     * Boards on this phone are not touched. They were made here and they stay
     * here -- the account was where they were copied to, not where they lived.
     */
    suspend fun erase(): Result<Unit>
}
