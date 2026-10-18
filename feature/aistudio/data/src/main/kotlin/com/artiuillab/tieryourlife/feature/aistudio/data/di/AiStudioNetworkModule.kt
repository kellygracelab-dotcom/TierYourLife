package com.artiuillab.tieryourlife.feature.aistudio.data.di

import com.artiuillab.tieryourlife.core.network.ProxyAddress
import com.artiuillab.tieryourlife.core.network.ProxyOkHttp
import com.artiuillab.tieryourlife.core.network.ProxyRetrofit
import com.artiuillab.tieryourlife.feature.account.domain.repository.GuestCredits
import com.artiuillab.tieryourlife.feature.aistudio.data.credits.ProxyGenerationCredits
import com.artiuillab.tieryourlife.feature.aistudio.data.credits.ProxyGuestCredits
import com.artiuillab.tieryourlife.feature.aistudio.data.credits.UnmeteredGenerationCredits
import com.artiuillab.tieryourlife.feature.aistudio.data.generation.ProxyCardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.data.generation.StubCardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiStudioNetworkModule {

    /** The proxy's own client and identity, given the minutes a generated picture can take. */
    @Provides
    @Singleton
    fun provideCardImageApi(
        @ProxyRetrofit retrofit: Retrofit,
        @ProxyOkHttp client: OkHttpClient,
    ): CardImageApi {
        val patient = client.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        return retrofit.newBuilder().client(patient).build().create(CardImageApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGuestCredits(implementation: ProxyGuestCredits): GuestCredits = implementation

    @Provides
    @Singleton
    fun provideCardImageGenerator(
        proxy: Provider<ProxyCardImageGenerator>,
        stub: Provider<StubCardImageGenerator>,
    ): CardImageGenerator {
        return if (ProxyAddress.isConfigured) proxy.get() else stub.get()
    }

    @Provides
    @Singleton
    fun provideGenerationCredits(
        metered: Provider<ProxyGenerationCredits>,
        unmetered: Provider<UnmeteredGenerationCredits>,
    ): GenerationCredits {
        return if (ProxyAddress.isConfigured) metered.get() else unmetered.get()
    }

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 300L
    private const val CALL_TIMEOUT_SECONDS = 330L
}
