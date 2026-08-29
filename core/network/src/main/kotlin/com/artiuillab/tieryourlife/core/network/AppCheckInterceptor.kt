package com.artiuillab.tieryourlife.core.network

import okhttp3.Interceptor
import okhttp3.Response

internal const val APP_CHECK_HEADER = "X-Firebase-AppCheck"

/**
 * The proxy turns away every call that arrives without this header, so a
 * failure here shows up as "nothing works" rather than as itself. That is
 * exactly what it did for an evening, which is why [token] is a parameter now:
 * the rule about what to do when there is no token is worth a test, and
 * Firebase's singleton cannot be reached from one.
 */
class AppCheckInterceptor(private val token: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Sent without the header rather than not sent at all: the refusal that
        // comes back is the server's to make, and it names the reason.
        val request = token()
            ?.takeIf { it.isNotEmpty() }
            ?.let { chain.request().newBuilder().header(APP_CHECK_HEADER, it).build() }
            ?: chain.request()
        return chain.proceed(request)
    }
}
