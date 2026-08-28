package com.artiuillab.tieryourlife.feature.account.domain.repository

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    val account: Flow<Account>

    suspend fun signInWithGoogle(idToken: String): SignInOutcome

    suspend fun signOut()
}
