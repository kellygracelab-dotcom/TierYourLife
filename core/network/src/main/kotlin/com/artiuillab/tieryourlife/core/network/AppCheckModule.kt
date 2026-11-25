package com.artiuillab.tieryourlife.core.network

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppCheckModule {

    @Provides
    @Singleton
    fun provideCallerTokens(): CallerTokens = CallerTokens(
        appCheck = { FirebaseAppCheck.getInstance().getAppCheckToken(false).asFuture().thenApply { it.token } },
        idToken = {
            val auth = FirebaseAuth.getInstance()
            // A guest identity is made on the spot rather than at launch: most
            // sessions never call anything that needs one.
            val user = auth.currentUser
            val token = if (user != null) {
                user.getIdToken(false)
            } else {
                auth.signInAnonymously().continueWithTask { signedIn -> checkNotNull(signedIn.result.user).getIdToken(false) }
            }
            token.asFuture().thenApply { it.token }
        },
    )

    @Provides
    @Singleton
    fun provideAppCheckInterceptor(tokens: CallerTokens): AppCheckInterceptor = AppCheckInterceptor(tokens::appCheckToken)
}
