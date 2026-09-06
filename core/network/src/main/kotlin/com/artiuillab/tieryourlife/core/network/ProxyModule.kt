package com.artiuillab.tieryourlife.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/** The proxy, spoken to as this install and this person: App Check and the ID token on every call. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProxyRetrofit

/** Keeps Retrofit's builder happy in a checkout with no proxy configured; every call then fails as unreachable. */
private const val PLACEHOLDER_BASE_URL = "https://localhost/"
private const val JSON_MEDIA_TYPE = "application/json"

@Module
@InstallIn(SingletonComponent::class)
object ProxyModule {

    @ProxyRetrofit
    @Provides
    @Singleton
    fun provideProxyRetrofit(
        appCheckInterceptor: AppCheckInterceptor,
        idTokenInterceptor: IdTokenInterceptor,
    ): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(appCheckInterceptor)
            .addInterceptor(idTokenInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.PROXY_BASE_URL.ifBlank { PLACEHOLDER_BASE_URL })
            .client(client)
            .addConverterFactory(networkJson.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .build()
    }
}
