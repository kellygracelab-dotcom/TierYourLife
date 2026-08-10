package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

private const val WIKIDATA_HOST = "www.wikidata.org"
private const val COMMONS_HOST = "commons.wikimedia.org"
private const val WIKIDATA_QUERY_HOST = "query.wikidata.org"

class WikimediaUserAgentInterceptor(
    private val userAgent: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val host = originalRequest.url.host

        if (host != WIKIDATA_HOST && host != COMMONS_HOST && host != WIKIDATA_QUERY_HOST) {
            return chain.proceed(originalRequest)
        }

        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build()

        return chain.proceed(requestWithUserAgent)
    }
}
