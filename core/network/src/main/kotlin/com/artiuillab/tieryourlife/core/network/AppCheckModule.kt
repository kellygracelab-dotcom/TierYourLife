package com.artiuillab.tieryourlife.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppCheckModule {

    @Provides
    @Singleton
    fun provideAppCheckInterceptor(): AppCheckInterceptor = AppCheckInterceptor {
        runCatching {
            Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false)).token
        }.onFailure { Timber.w(it, "App Check token unavailable") }.getOrNull()
    }
}
