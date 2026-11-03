package com.artiuillab.tieryourlife.feature.tier.board.components

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What [ListArt] draws for one tile: its cover, or else its first pictures. */
data class ArtPictures(val coverImageUrl: String?, val previewImages: List<String>)

/** One picture as [ListArt] asks Coil for it in a tile of [width] by [height] pixels. */
data class ArtRequest(val url: String, val width: Int, val height: Int)

/** The cover fills the tile; a mosaic's pictures share it out row by row, rounded up so the decoded picture is never too small. */
fun artRequests(art: ArtPictures, width: Int, height: Int): List<ArtRequest> {
    val images = art.previewImages.distinct().take(MOSAIC_CELLS)
    return when {
        art.coverImageUrl != null -> listOf(ArtRequest(art.coverImageUrl, width, height))
        images.isEmpty() -> emptyList()
        else -> {
            val rows = mosaicRows(images.size)
            val cellHeight = ceilDiv(height, rows.size)
            var next = 0
            rows.flatMap { perRow -> List(perRow) { ArtRequest(images[next++], ceilDiv(width, perRow), cellHeight) } }
        }
    }
}

private fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b

/**
 * Warms Coil's caches with the pictures these tiles will ask for, once the
 * frame on screen is drawn, so a tile scrolled to or a tab switched to draws
 * from memory. Nothing waits for it. Where pictures wait for Wi-Fi and the
 * connection is metered, only what is already on the phone is warmed.
 */
@Composable
fun PrefetchArt(arts: List<ArtPictures>, width: Int, height: Int, picturesOnWifiOnly: () -> Boolean) {
    val context = LocalContext.current
    LaunchedEffect(arts, width, height) {
        if (arts.isEmpty() || width <= 0 || height <= 0) return@LaunchedEffect
        withFrameNanos { }
        prefetchArt(context, arts, width, height, picturesOnWifiOnly)
    }
}

suspend fun prefetchArt(context: Context, arts: List<ArtPictures>, width: Int, height: Int, picturesOnWifiOnly: () -> Boolean) {
    val download = withContext(Dispatchers.IO) { !picturesOnWifiOnly() || !context.onMeteredNetwork() }
    val loader = SingletonImageLoader.get(context)
    arts.flatMap { artRequests(it, width, height) }.distinctBy { it.url }.forEach { request ->
        loader.enqueue(
            ImageRequest.Builder(context)
                .data(request.url)
                .size(request.width, request.height)
                // ContentScale.Crop, as ListArt draws them.
                .scale(Scale.FILL)
                .networkCachePolicy(if (download) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .build(),
        )
    }
}

/** An unknown network counts as metered, as it does for picture sync. */
private fun Context.onMeteredNetwork(): Boolean = getSystemService<ConnectivityManager>()?.isActiveNetworkMetered ?: true
