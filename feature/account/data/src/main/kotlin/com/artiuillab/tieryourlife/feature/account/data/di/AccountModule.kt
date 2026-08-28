package com.artiuillab.tieryourlife.feature.account.data.di

import com.artiuillab.tieryourlife.feature.account.data.repository.FirebaseAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(implementation: FirebaseAccountRepository): AccountRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
