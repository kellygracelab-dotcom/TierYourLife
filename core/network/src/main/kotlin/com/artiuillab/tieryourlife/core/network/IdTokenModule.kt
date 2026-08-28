package com.artiuillab.tieryourlife.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IdTokenModule {

    @Provides
    @Singleton
    fun provideIdTokenInterceptor(): IdTokenInterceptor = IdTokenInterceptor()
}
