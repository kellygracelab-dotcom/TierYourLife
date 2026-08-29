package com.artiuillab.tieryourlife.core.network

import okhttp3.Interceptor
import okhttp3.Response

internal const val AUTHORIZATION_HEADER = "Authorization"

/**
 * Who is calling, for the endpoints that meter or write. [token] is a
 * parameter for the same reason as in [AppCheckInterceptor]: so the rule can
 * be tested without Firebase.
 */
class IdTokenInterceptor(private val token: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = token()
            ?.takeIf { it.isNotEmpty() }
            ?.let { chain.request().newBuilder().header(AUTHORIZATION_HEADER, "Bearer $it").build() }
            ?: chain.request()
        return chain.proceed(request)
    }
}
