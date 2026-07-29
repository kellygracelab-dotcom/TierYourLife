package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

private const val TMDB_HOST = "api.themoviedb.org"

class TmdbAuthInterceptor(
    private val readAccessToken: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.url.host != TMDB_HOST) {
            return chain.proceed(originalRequest)
        }

        val authorizedRequest = originalRequest.newBuilder()
            .header(
                "Authorization",
                "Bearer $readAccessToken",
            )
            .header("Accept", "application/json")
            .build()

        return chain.proceed(authorizedRequest)
    }
}