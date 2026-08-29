package com.artiuillab.tieryourlife.feature.account.presentation.signin

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.artiuillab.tieryourlife.feature.account.domain.signin.GoogleWebClientId
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GoogleCredentialResult {
    data class Token(val idToken: String) : GoogleCredentialResult
    data object Cancelled : GoogleCredentialResult
    data object NoGoogleAccount : GoogleCredentialResult
    data object Unavailable : GoogleCredentialResult
}

/**
 * Lives in the presentation layer because it opens a system sheet and needs the
 * activity showing it.
 */
interface GoogleCredential {
    suspend fun request(context: Context): GoogleCredentialResult
}

@Singleton
class CredentialManagerGoogleCredential @Inject constructor(
    @GoogleWebClientId private val webClientId: String,
) : GoogleCredential {

    override suspend fun request(context: Context): GoogleCredentialResult {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        return try {
            val response = CredentialManager.create(context).getCredential(
                context = context,
                request = GetCredentialRequest.Builder().addCredentialOption(option).build(),
            )
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            GoogleCredentialResult.Token(credential.idToken)
        } catch (_: GetCredentialCancellationException) {
            GoogleCredentialResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleCredentialResult.NoGoogleAccount
        } catch (e: Exception) {
            Timber.w(e, "The account picker failed")
            GoogleCredentialResult.Unavailable
        }
    }
}
