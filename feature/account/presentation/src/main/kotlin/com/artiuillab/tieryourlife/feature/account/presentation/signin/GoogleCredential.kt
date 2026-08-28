package com.artiuillab.tieryourlife.feature.account.presentation.signin

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import javax.inject.Singleton

private const val LOG_TAG = "Account"
private const val WEB_CLIENT_ID_RESOURCE = "default_web_client_id"

/** What the account picker came back with. */
sealed interface GoogleCredentialResult {
    data class Token(val idToken: String) : GoogleCredentialResult
    data object Cancelled : GoogleCredentialResult
    data object NoGoogleAccount : GoogleCredentialResult
    data object Unavailable : GoogleCredentialResult
}

/**
 * Asks Google which account to use. This lives in the presentation layer on
 * purpose: it opens a system sheet and needs the activity showing it, so it is
 * a piece of UI, not a repository.
 */
interface GoogleCredential {
    suspend fun request(context: Context): GoogleCredentialResult
}

@Singleton
class CredentialManagerGoogleCredential @Inject constructor() : GoogleCredential {

    override suspend fun request(context: Context): GoogleCredentialResult {
        val clientId = context.webClientId() ?: run {
            // Present only once google-services.json carries an OAuth client,
            // which it does not until Google sign-in is switched on in Firebase.
            Log.w(LOG_TAG, "No web client id: Google sign-in is not configured")
            return GoogleCredentialResult.Unavailable
        }

        // Existing accounts first: someone opening this screen almost always
        // has the account they mean already on the phone.
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()

        return try {
            val response = CredentialManager.create(context).getCredential(
                context = context,
                request = GetCredentialRequest.Builder().addCredentialOption(option).build(),
            )
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            GoogleCredentialResult.Token(credential.idToken)
        } catch (e: GetCredentialCancellationException) {
            GoogleCredentialResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleCredentialResult.NoGoogleAccount
        } catch (e: Exception) {
            Log.w(LOG_TAG, "The account picker failed", e)
            GoogleCredentialResult.Unavailable
        }
    }

    /**
     * Generated into the app module by the google-services plugin, so it is
     * looked up by name rather than through `R` — this module never sees that
     * `R`, and its absence is a state the screen has to handle anyway.
     */
    private fun Context.webClientId(): String? {
        val id = resources.getIdentifier(WEB_CLIENT_ID_RESOURCE, "string", packageName)
        return if (id == 0) null else getString(id).takeIf { it.isNotBlank() }
    }
}
