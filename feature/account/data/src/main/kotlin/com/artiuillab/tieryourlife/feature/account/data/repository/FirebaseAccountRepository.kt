package com.artiuillab.tieryourlife.feature.account.data.repository

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val GOOGLE_PROVIDER = "google.com"

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
                adoptGoogleProfile()
                return SignInOutcome.Success
            } catch (e: FirebaseAuthUserCollisionException) {
                Timber.i(e, "Google account already in use; signing into it instead")
                return signInToExisting(credential)
            } catch (e: Exception) {
                Timber.w(e, "Could not link the Google account")
                return SignInOutcome.Failed
            }
        }

        return try {
            auth.signInWithCredential(credential).await()
            adoptGoogleProfile()
            SignInOutcome.Success
        } catch (e: Exception) {
            Timber.w(e, "Could not sign in with Google")
            SignInOutcome.Failed
        }
    }

    private suspend fun signInToExisting(
        credential: AuthCredential,
    ): SignInOutcome = try {
        auth.signInWithCredential(credential).await()
        adoptGoogleProfile()
        SignInOutcome.SignedInToExistingAccount(creditsCarriedOver = false)
    } catch (e: Exception) {
        Timber.w(e, "Could not sign into the existing Google account")
        SignInOutcome.Failed
    }

    override suspend fun setDisplayName(name: String): Boolean {
        val user = auth.currentUser ?: return false
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false

        return try {
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(trimmed).build()).await()
            // The published author comes off the ID token, which only carries the
            // new name once Firebase mints a fresh one.
            user.getIdToken(true).await()
            true
        } catch (e: Exception) {
            Timber.w(e, "Could not save the display name")
            false
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        runCatching { auth.signInAnonymously().await() }
            .onFailure { Timber.w(it, "Could not return to a guest identity") }
    }

    /**
     * Linking a Google credential onto a guest leaves the top-level profile
     * empty — the name and picture arrive only inside the provider record. Copy
     * them across so the account has a face, and so the ID token carries the
     * name the community will show.
     */
    private suspend fun adoptGoogleProfile() {
        val user = auth.currentUser ?: return
        val google = user.providerData.firstOrNull { it.providerId == GOOGLE_PROVIDER } ?: return
        val name = user.displayName ?: google.displayName
        val photo = user.photoUrl ?: google.photoUrl
        if (user.displayName == name && user.photoUrl == photo) return

        try {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .setPhotoUri(photo)
                    .build(),
            ).await()
            user.getIdToken(true).await()
        } catch (e: Exception) {
            Timber.w(e, "Could not copy the Google profile across")
        }
    }
}

private fun FirebaseUser?.toAccount(): Account = when {
    this == null || isAnonymous -> Account.Guest
    else -> {
        val google = providerData.firstOrNull { it.providerId == GOOGLE_PROVIDER }
        Account.SignedIn(
            email = email ?: google?.email,
            photoUrl = (photoUrl ?: google?.photoUrl)?.toString(),
            displayName = displayName ?: google?.displayName,
        )
    }
}
