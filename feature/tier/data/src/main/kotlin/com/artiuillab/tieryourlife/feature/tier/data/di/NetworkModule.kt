package com.artiuillab.tieryourlife.feature.tier.data.di

import com.artiuillab.tieryourlife.feature.tier.data.BuildConfig
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbAuthInterceptor
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataSparqlApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikimediaUserAgentInterceptor
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
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WikidataRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WikidataSparqlRetrofit

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
    fun provideWikimediaUserAgentInterceptor(): WikimediaUserAgentInterceptor {
        return WikimediaUserAgentInterceptor(userAgent = WIKIMEDIA_USER_AGENT)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tmdbAuthInterceptor: TmdbAuthInterceptor,
        wikimediaUserAgentInterceptor: WikimediaUserAgentInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(tmdbAuthInterceptor)
            .addInterceptor(wikimediaUserAgentInterceptor)
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

    @WikidataRetrofit
    @Provides
    @Singleton
    fun provideWikidataRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(WIKIDATA_BASE_URL)
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
    fun provideWikidataApi(
        @WikidataRetrofit retrofit: Retrofit,
    ): WikidataApi {
        return retrofit.create(WikidataApi::class.java)
    }

    @WikidataSparqlRetrofit
    @Provides
    @Singleton
    fun provideWikidataSparqlRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(WIKIDATA_SPARQL_BASE_URL)
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
    fun provideWikidataSparqlApi(
        @WikidataSparqlRetrofit retrofit: Retrofit,
    ): WikidataSparqlApi {
        return retrofit.create(WikidataSparqlApi::class.java)
    }

    private const val TMDB_BASE_URL = "https://api.themoviedb.org/"
    private const val WIKIDATA_BASE_URL = "https://www.wikidata.org/w/"
    private const val WIKIDATA_SPARQL_BASE_URL = "https://query.wikidata.org/"
    private const val JSON_MEDIA_TYPE = "application/json"
    private const val WIKIMEDIA_USER_AGENT = "TierYourLife/1.0 (com.artiuillab.tieryourlife; Android)"
}
