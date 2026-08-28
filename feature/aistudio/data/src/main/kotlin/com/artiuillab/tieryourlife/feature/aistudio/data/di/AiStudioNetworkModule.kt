package com.artiuillab.tieryourlife.feature.aistudio.data.di

import com.artiuillab.tieryourlife.core.network.AppCheckInterceptor
import com.artiuillab.tieryourlife.core.network.IdTokenInterceptor
import com.artiuillab.tieryourlife.feature.aistudio.data.BuildConfig
import com.artiuillab.tieryourlife.feature.aistudio.data.credits.ProxyGenerationCredits
import com.artiuillab.tieryourlife.feature.aistudio.data.credits.UnmeteredGenerationCredits
import com.artiuillab.tieryourlife.feature.aistudio.data.generation.ProxyCardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.data.generation.StubCardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.networkJson
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProxyOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProxyRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AiStudioNetworkModule {

    @ProxyOkHttp
    @Provides
    @Singleton
    fun provideProxyOkHttpClient(
        appCheckInterceptor: AppCheckInterceptor,
        idTokenInterceptor: IdTokenInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(appCheckInterceptor)
            .addInterceptor(idTokenInterceptor)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @ProxyRetrofit
    @Provides
    @Singleton
    fun provideProxyRetrofit(
        @ProxyOkHttp okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.PROXY_BASE_URL.ifBlank { PLACEHOLDER_BASE_URL })
            .client(okHttpClient)
            .addConverterFactory(
                networkJson.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()),
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideCardImageApi(
        @ProxyRetrofit retrofit: Retrofit,
    ): CardImageApi {
        return retrofit.create(CardImageApi::class.java)
    }

    // One switch decides both: without a proxy the images are drawn on the
    // device, and there is nothing on the other side to keep a balance.
    @Provides
    @Singleton
    fun provideCardImageGenerator(
        proxy: Provider<ProxyCardImageGenerator>,
        stub: Provider<StubCardImageGenerator>,
    ): CardImageGenerator {
        return if (BuildConfig.PROXY_BASE_URL.isNotBlank()) proxy.get() else stub.get()
    }

    @Provides
    @Singleton
    fun provideGenerationCredits(
        metered: Provider<ProxyGenerationCredits>,
        unmetered: Provider<UnmeteredGenerationCredits>,
    ): GenerationCredits {
        return if (BuildConfig.PROXY_BASE_URL.isNotBlank()) metered.get() else unmetered.get()
    }

    private const val PLACEHOLDER_BASE_URL = "https://example.invalid/"
    private const val JSON_MEDIA_TYPE = "application/json"
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 300L
    private const val CALL_TIMEOUT_SECONDS = 330L
}
