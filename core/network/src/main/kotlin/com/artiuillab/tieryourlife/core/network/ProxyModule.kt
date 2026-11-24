package com.artiuillab.tieryourlife.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Optional
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

/**
 * OkHttp's five at a time to one host was the whole app: every call is to
 * the proxy, and a list opened behind sync and pictures waited its turn.
 */
internal const val PROXY_CALLS_AT_ONCE = 16

@Module
@InstallIn(SingletonComponent::class)
object ProxyModule {

    @ProxyOkHttp
    @Provides
    @Singleton
    fun provideProxyOkHttpClient(
        appCheckInterceptor: AppCheckInterceptor,
        idTokenInterceptor: IdTokenInterceptor,
        erasedAccounts: ErasedAccounts,
        headers: Optional<ProxyHeaders> = Optional.empty(),
    ): OkHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply { maxRequestsPerHost = PROXY_CALLS_AT_ONCE })
        .addInterceptor(ProxyHeadersInterceptor(headers.orElse(null)))
        .addInterceptor(appCheckInterceptor)
        .addInterceptor(idTokenInterceptor)
        // After the token, so it sees whose token the refusal answered.
        .addInterceptor(AccountErasedInterceptor(erasedAccounts::report))
        .build()

    @ProxyRetrofit
    @Provides
    @Singleton
    fun provideProxyRetrofit(@ProxyOkHttp client: OkHttpClient): Retrofit = proxyRetrofit(ProxyAddress.resolve(), client)

    /** The end-to-end tests build the same one over the emulators' address. */
    fun proxyRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(networkJson.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .build()
}
