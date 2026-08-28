package com.artiuillab.tieryourlife.feature.account.domain.repository

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    val account: Flow<Account>

    /**
     * Attaches a Google identity to the one already in use, so the credits
     * counted against it survive a reinstall. Takes a token rather than doing
     * the picking itself: choosing an account is a screen, not a repository.
     */
    suspend fun signInWithGoogle(idToken: String): SignInOutcome

    /** Returns to a guest identity. Never leaves the app without one. */
    suspend fun signOut()
}
