package com.artiuillab.tieryourlife.feature.account.data.di

import com.artiuillab.tieryourlife.core.network.ProxyRetrofit
import com.artiuillab.tieryourlife.feature.account.data.remote.AccountApi
import com.artiuillab.tieryourlife.feature.account.data.repository.FirebaseAccountRepository
import com.artiuillab.tieryourlife.feature.account.data.repository.RetrofitAccountErasure
import com.artiuillab.tieryourlife.feature.account.data.repository.StorageFacePhotos
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountErasure
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotos
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(implementation: FirebaseAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun bindAccountErasure(implementation: RetrofitAccountErasure): AccountErasure

    @Binds
    @Singleton
    abstract fun bindFacePhotos(implementation: StorageFacePhotos): FacePhotos

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideAccountApi(@ProxyRetrofit retrofit: Retrofit): AccountApi =
            retrofit.create(AccountApi::class.java)
    }
}
