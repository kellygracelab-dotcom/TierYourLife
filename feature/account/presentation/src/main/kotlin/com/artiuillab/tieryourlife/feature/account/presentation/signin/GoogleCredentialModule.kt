package com.artiuillab.tieryourlife.feature.account.presentation.signin

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GoogleCredentialModule {

    @Binds
    @Singleton
    abstract fun bindGoogleCredential(
        implementation: CredentialManagerGoogleCredential,
    ): GoogleCredential
}
