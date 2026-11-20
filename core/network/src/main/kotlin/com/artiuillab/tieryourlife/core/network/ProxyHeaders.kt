package com.artiuillab.tieryourlife.core.network

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.Response

/** Headers for every call to the proxy, asked afresh each call: none at all is an answer. */
fun interface ProxyHeaders {
    fun headers(): Map<String, String>
}

class ProxyHeadersInterceptor(private val headers: ProxyHeaders?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val extra = headers?.headers().orEmpty()
        if (extra.isEmpty()) return chain.proceed(chain.request())
        val request = chain.request().newBuilder().apply { extra.forEach { (name, value) -> header(name, value) } }.build()
        return chain.proceed(request)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ProxyHeadersModule {
    @BindsOptionalOf
    abstract fun proxyHeaders(): ProxyHeaders
}
