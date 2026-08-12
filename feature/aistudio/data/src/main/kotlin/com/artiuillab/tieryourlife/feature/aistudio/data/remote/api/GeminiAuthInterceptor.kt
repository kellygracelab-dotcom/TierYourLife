package com.artiuillab.tieryourlife.feature.aistudio.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

private const val GEMINI_HOST = "generativelanguage.googleapis.com"

class GeminiAuthInterceptor(
    private val apiKey: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.url.host != GEMINI_HOST) {
            return chain.proceed(originalRequest)
        }

        val authorizedRequest = originalRequest.newBuilder()
            .header("x-goog-api-key", apiKey)
            .build()

        return chain.proceed(authorizedRequest)
    }
}
