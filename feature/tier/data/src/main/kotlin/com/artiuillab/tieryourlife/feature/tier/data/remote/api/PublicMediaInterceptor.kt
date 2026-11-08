package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

private val STORAGE_PREFIX = "https://firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o".toHttpUrl()
private const val PUBLIC_ORIGIN = "https://tieryourlife.web.app"
private const val PUBLIC_PATH_PARTS = 3
private const val MAX_OBJECT_PATH_LENGTH = 1024
private val PUBLIC_KINDS = setOf("published", "avatars", "sections", "topics")
private val ID = Regex("[A-Za-z0-9_-]{1,128}")
private val PICTURE = Regex("[A-Za-z0-9_-]+")

/** Rewrites only the network read: stored board URLs and their fingerprints stay unchanged. */
class PublicMediaInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.method != "GET" && original.method != "HEAD") return chain.proceed(original)
        val publicUrl = publicUrl(original.url) ?: return chain.proceed(original)
        return chain.proceed(
            original.newBuilder()
                .url(publicUrl)
                .removeHeader("Authorization")
                .removeHeader("Cookie")
                .removeHeader("X-Firebase-AppCheck")
                .build(),
        )
    }
}

private fun publicUrl(url: HttpUrl): HttpUrl? {
    if (url.scheme != STORAGE_PREFIX.scheme || url.host != STORAGE_PREFIX.host || url.port != STORAGE_PREFIX.port) return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.queryParameter("alt") != "media") return null
    val segments = url.pathSegments
    if (segments.size != STORAGE_PREFIX.pathSegments.size + 1 || segments.dropLast(1) != STORAGE_PREFIX.pathSegments) return null
    val path = segments.last()
    val parts = path.split('/')
    if (path.length > MAX_OBJECT_PATH_LENGTH || parts.size != PUBLIC_PATH_PARTS || parts[0] !in PUBLIC_KINDS) return null
    if (!ID.matches(parts[1]) || !PICTURE.matches(parts[2])) return null
    return "$PUBLIC_ORIGIN/img/original/$path".toHttpUrl()
}
