package com.artiuillab.tieryourlife.feature.account.data.repository

import android.util.Log
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val LOG_TAG = "Account"

@Singleton
class FirebaseAccountRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : AccountRepository {

    override val account: Flow<Account> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser.toAccount()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signInWithGoogle(idToken: String): SignInOutcome {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val guest = auth.currentUser?.takeIf { it.isAnonymous }

        if (guest != null) {
            try {
                guest.linkWithCredential(credential).await()
                return SignInOutcome.Success
            } catch (e: FirebaseAuthUserCollisionException) {
                Log.i(LOG_TAG, "Google account already in use; signing into it instead", e)
                return signInToExisting(credential)
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Could not link the Google account", e)
                return SignInOutcome.Failed
            }
        }

        return try {
            auth.signInWithCredential(credential).await()
            SignInOutcome.Success
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Could not sign in with Google", e)
            SignInOutcome.Failed
        }
    }

    private suspend fun signInToExisting(
        credential: AuthCredential,
    ): SignInOutcome = try {
        auth.signInWithCredential(credential).await()
        SignInOutcome.SignedInToExistingAccount(creditsCarriedOver = false)
    } catch (e: Exception) {
        Log.w(LOG_TAG, "Could not sign into the existing Google account", e)
        SignInOutcome.Failed
    }

    override suspend fun signOut() {
        auth.signOut()
        runCatching { auth.signInAnonymously().await() }
            .onFailure { Log.w(LOG_TAG, "Could not return to a guest identity", it) }
    }
}

private fun FirebaseUser?.toAccount(): Account = when {
    this == null || isAnonymous -> Account.Guest
    else -> Account.SignedIn(email = email, photoUrl = photoUrl?.toString())
}
