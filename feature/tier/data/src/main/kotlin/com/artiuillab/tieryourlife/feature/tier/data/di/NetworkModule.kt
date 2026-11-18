package com.artiuillab.tieryourlife.feature.tier.data.di

import com.artiuillab.tieryourlife.core.network.ProxyRetrofit
import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikimediaUserAgentInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = networkJson

    @Provides
    @Singleton
    fun provideWikimediaUserAgentInterceptor(): WikimediaUserAgentInterceptor {
        return WikimediaUserAgentInterceptor(userAgent = WIKIMEDIA_USER_AGENT)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        wikimediaUserAgentInterceptor: WikimediaUserAgentInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(wikimediaUserAgentInterceptor)
            .build()
    }

    // The same client as the community: both count against an account, and
    // both are refused without one.
    @Provides
    @Singleton
    fun provideBoardsApi(@ProxyRetrofit retrofit: Retrofit): BoardsApi =
        retrofit.create(BoardsApi::class.java)

    private const val WIKIMEDIA_USER_AGENT = "TierYourLife/1.0 (com.artiuillab.tieryourlife; Android)"
}
