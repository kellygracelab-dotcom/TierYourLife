package com.artiuillab.tieryourlife.feature.account.domain.repository

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    val account: Flow<Account>

    suspend fun signInWithGoogle(idToken: String): SignInOutcome

    /** Renames the author the community shows. Answers whether it stuck. */
    suspend fun setDisplayName(name: String): Boolean

    /**
     * Sets the face the community shows. [photoUrl] must be an https address we
     * do not host -- null falls back to the letter. Answers whether it stuck.
     */
    suspend fun setPhotoUrl(photoUrl: String?): Boolean

    /** The photo Google gave, whatever the user has chosen since. */
    fun googlePhotoUrl(): String?

    suspend fun signOut()
}
