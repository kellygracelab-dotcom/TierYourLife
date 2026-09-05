package com.artiuillab.tieryourlife.feature.account.data.repository

import androidx.core.net.toUri
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.repository.GuestCredits
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val GOOGLE_PROVIDER = "google.com"

@Singleton
class FirebaseAccountRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val guestCredits: GuestCredits,
) : AccountRepository {

    /** Renaming or changing the picture leaves the auth state alone, so those edits say so here. */
    private val profileEdits = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val account: Flow<Account> = merge(
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(Unit) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        },
        profileEdits,
    ).map { auth.currentUser.toAccount() }.distinctUntilChanged()

    override suspend fun signInWithGoogle(idToken: String): SignInOutcome {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val guest = auth.currentUser?.takeIf { it.isAnonymous }

        if (guest != null) {
            // Taken before anything is attempted: if the sign-in becomes a switch
            // of identity, this is the only proof left that the guest was this person.
            val guestToken = runCatching { guest.getIdToken(false).await().token }.getOrNull()
            try {
                guest.linkWithCredential(credential).await()
                adoptGoogleProfile()
                return SignInOutcome.Success
            } catch (e: FirebaseAuthUserCollisionException) {
                Timber.i(e, "Google account already in use; signing into it instead")
                return signInToExisting(credential, guestToken)
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
        guestToken: String?,
    ): SignInOutcome = try {
        auth.signInWithCredential(credential).await()
        adoptGoogleProfile()
        // The guest is gone from this phone but not from the ledger; the last
        // moment anybody can say whose balance it was.
        val carried = guestToken != null && guestCredits.carryOver(guestToken)
        SignInOutcome.SignedInToExistingAccount(creditsCarriedOver = carried)
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
            profileEdits.tryEmit(Unit)
            true
        } catch (e: Exception) {
            Timber.w(e, "Could not save the display name")
            false
        }
    }

    /** In the Firebase profile, so it reaches the proxy in the ID token. */
    override suspend fun setPhotoUrl(photoUrl: String?): Boolean {
        val user = auth.currentUser ?: return false
        val uri = photoUrl?.takeIf { it.startsWith("https://") }?.toUri()

        return try {
            user.updateProfile(UserProfileChangeRequest.Builder().setPhotoUri(uri).build()).await()
            user.getIdToken(true).await()
            profileEdits.tryEmit(Unit)
            true
        } catch (e: Exception) {
            Timber.w(e, "Could not save the profile photo")
            false
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        runCatching { auth.signInAnonymously().await() }
            .onFailure { Timber.w(it, "Could not return to a guest identity") }
    }

    /** Linking a Google credential onto a guest leaves the top-level profile empty; the name and picture arrive only inside the provider record. */
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
            profileEdits.tryEmit(Unit)
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
