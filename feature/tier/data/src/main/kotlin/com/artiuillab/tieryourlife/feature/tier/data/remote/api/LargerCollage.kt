package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.pxOrElse
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The web's cover collage is 480×300; `s=2` gives it at 960×600 (Web, 28.11). */
internal const val COLLAGE_WIDTH_PX = 480
internal const val COLLAGE_HEIGHT_PX = 300
private val WEB_HOSTS = setOf("tieryourlife.com", "tieryourlife.web.app")
private val COLLAGE_PATH = listOf("img", "collage")
private val COLLAGE_FILE = Regex("[A-Za-z0-9_-]{1,128}\\.webp")

/**
 * A list's cover collage drawn larger than 480×300 on this screen comes at
 * twice that, rather than stretched across a card as wide as a phone. One that
 * does not come gives way to the usual size.
 */
class LargerCollage : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val width = chain.size.width.pxOrElse { 0 }
        val height = chain.size.height.pxOrElse { 0 }
        if (width <= COLLAGE_WIDTH_PX && height <= COLLAGE_HEIGHT_PX) return chain.proceed()
        val url = (request.data as? String)?.toHttpUrlOrNull() ?: return chain.proceed()
        val larger = largerCollageUrl(url) ?: return chain.proceed()
        val came = chain.withRequest(request.newBuilder().data(larger).build()).proceed()
        return if (came is SuccessResult) came else chain.proceed()
    }
}

/** The same collage at twice the size; null for anything that is not one of ours. */
internal fun largerCollageUrl(url: HttpUrl): String? {
    val ours = url.scheme == "https" && url.host in WEB_HOSTS && url.username.isEmpty() && url.password.isEmpty()
    val query = url.querySize == 0 || url.queryParameterNames == setOf("v")
    if (!ours || !query || url.pathSegments != COLLAGE_PATH + url.pathSegments.last()) return null
    if (!COLLAGE_FILE.matches(url.pathSegments.last())) return null
    return url.newBuilder().setQueryParameter("s", "2").build().toString()
}
