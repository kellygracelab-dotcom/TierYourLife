package com.artiuillab.tieryourlife.feature.account.domain.repository

/**
 * A different promise from [AccountRepository]: signing out is undone by
 * signing in; this removes the account, its copy of the boards, its published
 * lists and their pictures, and cannot be undone.
 */
interface AccountErasure {

    /** Boards on this phone are not touched: the account was where they were copied to, not where they lived. */
    suspend fun erase(): Result<Unit>
}
