package com.artiuillab.tieryourlife.feature.account.domain.repository

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    val account: Flow<Account>

    suspend fun signInWithGoogle(idToken: String): SignInOutcome

    /** Renames the author the community shows. Answers whether it stuck. */
    suspend fun setDisplayName(name: String): Boolean

    suspend fun signOut()
}
