package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.size.Size
import coil3.size.pxOrElse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The web's copies of a published card picture, by width. */
internal val CARD_COPY_WIDTHS = listOf(200, 400)

/**
 * A published list's card picture comes as the smallest of the web's copies
 * that covers the tile on this screen, in pixels; bigger than the largest, it
 * stays the original. A full-size picture shown at a tile's size made a list
 * of 177 cards 28 MB on the site (Designer, 25.11). A copy that does not come,
 * missing or failed, gives way to the original, so no card stays grey.
 */
class SmallCardCopies : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val width = copyWidthFor(chain.size) ?: return chain.proceed()
        val copy = (request.data as? String)?.toHttpUrlOrNull()?.let { publishedCopyUrl(it, width) } ?: return chain.proceed()
        val small = chain.withRequest(request.newBuilder().data(copy).build()).proceed()
        return if (small is ErrorResult) chain.proceed() else small
    }
}

/**
 * The copy for a tile of [size]: its longer side, so a picture cropped to fill
 * a tall tile is not stretched; null for an unknown size or one past the largest copy.
 */
internal fun copyWidthFor(size: Size): Int? {
    val longer = maxOf(size.width.pxOrElse { 0 }, size.height.pxOrElse { 0 })
    if (longer <= 0) return null
    return CARD_COPY_WIDTHS.firstOrNull { longer <= it }
}
