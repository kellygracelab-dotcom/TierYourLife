package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * How long a picture search is waited for. The quick part answers from the fast catalogues, each cut off at
 * 5 s on the server (9 s at worst measured); the whole one waits for Open Library and the web too, and has
 * taken 25 s. The client's usual 10 s cut the whole one short, and the person saw nothing more.
 */
internal object PictureSearchTimeouts : Interceptor {
    const val QUICK_SECONDS = 20L
    const val WHOLE_SECONDS = 45L
    private const val PART = "part"
    private const val QUICK = "quick"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val seconds = if (request.url.queryParameter(PART) == QUICK) QUICK_SECONDS else WHOLE_SECONDS
        return chain.withReadTimeout(seconds.toInt(), TimeUnit.SECONDS).proceed(request)
    }

    /** [client] with these waits for every search it sends. */
    fun on(client: OkHttpClient): OkHttpClient = client.newBuilder().addInterceptor(this).build()
}
