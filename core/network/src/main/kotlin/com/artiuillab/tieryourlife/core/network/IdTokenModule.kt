package com.artiuillab.tieryourlife.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IdTokenModule {

    @Provides
    @Singleton
    fun provideIdTokenInterceptor(): IdTokenInterceptor = IdTokenInterceptor {
        runCatching {
            val auth = FirebaseAuth.getInstance()
            // A guest identity is made on the spot rather than at launch: most
            // sessions never call anything that needs one.
            val user = auth.currentUser ?: Tasks.await(auth.signInAnonymously()).user
            user?.let { Tasks.await(it.getIdToken(false)).token }
        }.onFailure { Timber.w(it, "ID token unavailable") }.getOrNull()
    }
}
