package com.artiuillab.tieryourlife.feature.tier.data.di

import com.artiuillab.tieryourlife.core.network.ProxyRetrofit
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PicturesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Picture search goes through the account's own client: the server counts web searches per person. */
@Module
@InstallIn(SingletonComponent::class)
object PicturesModule {

    @Provides
    @Singleton
    fun providePicturesApi(@ProxyRetrofit retrofit: Retrofit): PicturesApi =
        retrofit.create(PicturesApi::class.java)
}
