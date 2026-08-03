package com.artiuillab.tieryourlife.feature.tier.data.di

import com.artiuillab.tieryourlife.feature.tier.data.BuildConfig
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbAuthInterceptor
import com.artiuillab.tieryourlife.feature.tier.data.remote.networkJson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = networkJson

    @Provides
    @Singleton
    fun provideTmdbAuthInterceptor(): TmdbAuthInterceptor {
        return TmdbAuthInterceptor(
            readAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN,
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: TmdbAuthInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory(
                    JSON_MEDIA_TYPE.toMediaType(),
                ),
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(
        retrofit: Retrofit,
    ): TmdbApi {
        return retrofit.create(TmdbApi::class.java)
    }

    private const val TMDB_BASE_URL = "https://api.themoviedb.org/"
    private const val JSON_MEDIA_TYPE = "application/json"
}