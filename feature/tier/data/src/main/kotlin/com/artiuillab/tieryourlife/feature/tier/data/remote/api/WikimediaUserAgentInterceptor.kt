package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.InterruptedIOException

private val WIKIMEDIA_DOMAINS = listOf("wikimedia.org", "wikipedia.org", "wikidata.org")
private const val TOO_MANY_REQUESTS = 429
private const val MAX_WAIT_SECONDS = 2L
private const val MILLIS_PER_SECOND = 1000L

/**
 * Wikimedia refuses okhttp's own User-Agent with 403, pictures on upload.wikimedia.org included.
 * A board asks for many thumbnails at once, and Wikimedia may answer 429 with a short Retry-After:
 * that wait is kept, once.
 */
class WikimediaUserAgentInterceptor(
    private val userAgent: String,
    private val pause: (millis: Long) -> Unit = Thread::sleep,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isWikimedia(request.url.host)) return chain.proceed(request)
        val identified = request.newBuilder().header("User-Agent", userAgent).build()
        val response = chain.proceed(identified)
        val wait = response.shortRetryAfterSeconds() ?: return response
        if (chain.call().isCanceled()) return response
        response.close()
        try {
            pause(wait * MILLIS_PER_SECOND)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Interrupted while waiting for Wikimedia").apply { initCause(interrupted) }
        }
        return chain.proceed(identified)
    }

    private fun Response.shortRetryAfterSeconds(): Long? {
        if (code != TOO_MANY_REQUESTS) return null
        return header("Retry-After")?.trim()?.toLongOrNull()?.takeIf { it in 0..MAX_WAIT_SECONDS }
    }

    private fun isWikimedia(host: String): Boolean =
        WIKIMEDIA_DOMAINS.any { host == it || host.endsWith(".$it") }
}
