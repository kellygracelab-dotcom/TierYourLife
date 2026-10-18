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

/** The client behind [ProxyRetrofit], for a feature that needs the same identity with other timeouts. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProxyOkHttp

private const val JSON_MEDIA_TYPE = "application/json"

@Module
@InstallIn(SingletonComponent::class)
object ProxyModule {

    @ProxyOkHttp
    @Provides
    @Singleton
    fun provideProxyOkHttpClient(
        appCheckInterceptor: AppCheckInterceptor,
        idTokenInterceptor: IdTokenInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(appCheckInterceptor)
        .addInterceptor(idTokenInterceptor)
        .build()

    @ProxyRetrofit
    @Provides
    @Singleton
    fun provideProxyRetrofit(@ProxyOkHttp client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(ProxyAddress.resolve())
            .client(client)
            .addConverterFactory(networkJson.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .build()
}
