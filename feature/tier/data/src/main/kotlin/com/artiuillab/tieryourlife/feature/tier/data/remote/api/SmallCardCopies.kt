package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.size.pxOrElse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlin.math.ceil

/** The web's copies of a published card picture, by width. */
internal val CARD_COPY_WIDTHS = listOf(200, 400, 800)

/** A copy's height is rounded on the server, a decoded size on the phone. */
private const val ROUNDING_PX = 1

/**
 * A published list's card picture comes as the smallest of the web's copies
 * that covers the tile on this screen, in pixels; bigger than the largest, it
 * stays the original. A full-size picture shown at a tile's size made a list
 * of 177 cards 28 MB on the site (Designer, 25.11). A copy that does not come,
 * missing or failed, gives way to the original, so no card stays grey.
 *
 * The copies are cut by width, so a wide picture cropped into a tall tile
 * needs a wider copy than the tile: the first one that came tells its shape,
 * and one that falls short is followed by the copy, or the original, that
 * does cover it, rather than stretched (Danylo, 28.11: "не мыльно").
 */
class SmallCardCopies : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val tile = Tile.of(chain.size) ?: return chain.proceed()
        val url = (request.data as? String)?.toHttpUrlOrNull() ?: return chain.proceed()
        var width = copyWidthFor(chain.size)
        while (width != null) {
            val copy = publishedCopyUrl(url, width) ?: return chain.proceed()
            // Never enlarged on decoding, so the picture's own size tells whether it covers.
            val came = chain.withRequest(request.newBuilder().data(copy).precision(Precision.INEXACT).build()).proceed()
            if (came !is SuccessResult) return chain.proceed()
            when (val next = nextFor(came.image.width, came.image.height, width, tile, request.scale)) {
                Cover.Enough -> return came
                Cover.Original -> return chain.proceed()
                is Cover.Copy -> width = next.width
            }
        }
        return chain.proceed()
    }
}

internal data class Tile(val width: Int, val height: Int) {
    companion object {
        fun of(size: Size): Tile? {
            val width = size.width.pxOrElse { 0 }
            val height = size.height.pxOrElse { 0 }
            return if (width > 0 && height > 0) Tile(width, height) else null
        }
    }
}

internal sealed interface Cover {
    data object Enough : Cover
    data object Original : Cover
    data class Copy(val width: Int) : Cover
}

/**
 * The first copy to ask for a tile of [size]: wide enough for its longer side,
 * which covers any picture no wider than it is tall; null for an unknown size
 * or one past the largest copy.
 */
internal fun copyWidthFor(size: Size): Int? {
    val longer = maxOf(size.width.pxOrElse { 0 }, size.height.pxOrElse { 0 })
    if (longer <= 0) return null
    return CARD_COPY_WIDTHS.firstOrNull { longer <= it }
}

/**
 * After the copy [asked] wide came as [width]×[height]: whether it covers the
 * [tile] without being enlarged, and if not, the copy that would. A copy
 * narrower than asked is already the whole original.
 */
internal fun nextFor(width: Int, height: Int, asked: Int, tile: Tile, scale: Scale): Cover {
    if (width <= 0 || height <= 0) return Cover.Enough
    val covers = when (scale) {
        Scale.FILL -> width + ROUNDING_PX >= tile.width && height + ROUNDING_PX >= tile.height
        Scale.FIT -> width + ROUNDING_PX >= tile.width || height + ROUNDING_PX >= tile.height
    }
    if (covers || width < asked) return Cover.Enough
    val widthForHeight = ceil(tile.height.toDouble() * width / height).toInt()
    val needed = when (scale) {
        Scale.FILL -> maxOf(tile.width, widthForHeight)
        Scale.FIT -> minOf(tile.width, widthForHeight)
    }
    return CARD_COPY_WIDTHS.firstOrNull { it > asked && it >= needed }?.let { Cover.Copy(it) } ?: Cover.Original
}
