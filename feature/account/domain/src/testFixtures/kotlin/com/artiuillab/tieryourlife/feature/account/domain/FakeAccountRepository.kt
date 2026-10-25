package com.artiuillab.tieryourlife.feature.account.domain

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Signs in with whatever [outcome] says and records the tokens it was handed. */
open class FakeAccountRepository(
    initial: Account = Account.Guest,
    private val outcome: SignInOutcome = SignInOutcome.Success,
    private val onSignOut: () -> Unit = {},
) : AccountRepository {

    val tokens = mutableListOf<String>()
    val photoUrls = mutableListOf<String?>()
    val accounts = MutableStateFlow(initial)

    override val account: Flow<Account> = accounts

    override suspend fun signInWithGoogle(idToken: String): SignInOutcome {
        tokens += idToken
        if (outcome is SignInOutcome.Success) {
            accounts.value = Account.SignedIn(email = SIGNED_IN_EMAIL, photoUrl = null)
        }
        return outcome
    }

    override suspend fun setDisplayName(name: String): Boolean = true

    override suspend fun setPhotoUrl(photoUrl: String?): Boolean {
        photoUrls += photoUrl
        return true
    }

    override suspend fun signOut() {
        onSignOut()
        accounts.value = Account.Guest
    }

    companion object {
        const val SIGNED_IN_EMAIL = "someone@example.com"
        val signedIn: Account = Account.SignedIn(email = SIGNED_IN_EMAIL, photoUrl = null)
    }
}
