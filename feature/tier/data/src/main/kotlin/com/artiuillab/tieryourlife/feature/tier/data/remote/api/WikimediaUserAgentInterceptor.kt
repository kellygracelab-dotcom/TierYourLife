package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

private val WIKIMEDIA_DOMAINS = listOf("wikimedia.org", "wikipedia.org", "wikidata.org")

/** Wikimedia refuses okhttp's own User-Agent with 403, pictures on upload.wikimedia.org included. */
class WikimediaUserAgentInterceptor(
    private val userAgent: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isWikimedia(request.url.host)) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("User-Agent", userAgent).build())
    }

    private fun isWikimedia(host: String): Boolean =
        WIKIMEDIA_DOMAINS.any { host == it || host.endsWith(".$it") }
}
