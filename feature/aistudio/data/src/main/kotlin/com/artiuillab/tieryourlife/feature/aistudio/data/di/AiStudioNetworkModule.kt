package com.artiuillab.tieryourlife.feature.aistudio.data.di

import com.artiuillab.tieryourlife.feature.aistudio.data.BuildConfig
import com.artiuillab.tieryourlife.feature.aistudio.data.generation.GeminiCardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.data.generation.StubCardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.GeminiAuthInterceptor
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.GeminiImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.networkJson
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AiStudioNetworkModule {

    @Provides
    @Singleton
    fun provideGeminiAuthInterceptor(): GeminiAuthInterceptor {
        return GeminiAuthInterceptor(apiKey = BuildConfig.GEMINI_API_KEY)
    }

    @GeminiOkHttp
    @Provides
    @Singleton
    fun provideGeminiOkHttpClient(
        geminiAuthInterceptor: GeminiAuthInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(geminiAuthInterceptor)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @GeminiRetrofit
    @Provides
    @Singleton
    fun provideGeminiRetrofit(
        @GeminiOkHttp okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                networkJson.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()),
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiImageApi(
        @GeminiRetrofit retrofit: Retrofit,
    ): GeminiImageApi {
        return retrofit.create(GeminiImageApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCardImageGenerator(
        gemini: Provider<GeminiCardImageGenerator>,
        stub: Provider<StubCardImageGenerator>,
    ): CardImageGenerator {
        return if (BuildConfig.GEMINI_API_KEY.isNotBlank()) gemini.get() else stub.get()
    }

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val JSON_MEDIA_TYPE = "application/json"
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 120L
    private const val CALL_TIMEOUT_SECONDS = 150L
}
