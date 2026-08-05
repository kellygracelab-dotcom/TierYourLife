package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

private const val WIKIDATA_HOST = "www.wikidata.org"
private const val COMMONS_HOST = "commons.wikimedia.org"

// Wikimedia's API etiquette policy requires a descriptive User-Agent and throttles requests
// without one. Host-gated the same way TmdbAuthInterceptor gates its Authorization header, so
// the two can share one OkHttpClient without either header leaking to the other's host.
class WikimediaUserAgentInterceptor(
    private val userAgent: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val host = originalRequest.url.host

        if (host != WIKIDATA_HOST && host != COMMONS_HOST) {
            return chain.proceed(originalRequest)
        }

        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build()

        return chain.proceed(requestWithUserAgent)
    }
}
