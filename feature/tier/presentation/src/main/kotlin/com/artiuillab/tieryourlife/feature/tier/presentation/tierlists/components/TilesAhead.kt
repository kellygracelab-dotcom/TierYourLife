package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.artiuillab.tieryourlife.feature.tier.board.components.ArtPictures
import com.artiuillab.tieryourlife.feature.tier.board.components.BoardCollages
import com.artiuillab.tieryourlife.feature.tier.board.components.LocalBoardCollages
import com.artiuillab.tieryourlife.feature.tier.board.components.boardCollageOf
import com.artiuillab.tieryourlife.feature.tier.board.components.prefetchArt
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val TILES_AHEAD = 6

/** A tile whose collage is on the phone needs none of its pictures: it shows that one (as tall as aspectRatio rounds it). */
private suspend fun TierListSummary.collageKept(collages: BoardCollages, width: Int): Boolean {
    val art = art()
    if (art.coverImageUrl != null) return false
    val collage = boardCollageOf(id.toString(), art.previewImages, art.pictureFocus, width, (width / ART_RATIO).roundToInt())
        ?: return false
    return (collages.known(collage) ?: collages.find(collage)) != null
}

/** What a [BoardTile] draws: a local photograph as it is on the board, since only publishing needs a picture to have travelled. */
internal fun TierListSummary.art() = ArtPictures(coverImageUrl, coverImageUrls.ifEmpty { previewImages }, coverFocus, pictureFocus)

/**
 * The tiles just under the last one on screen, fetched once the first frame
 * is drawn, at the size the tile on screen measured: those on screen are
 * already asking for their own. [tiles] are in grid order, each by its key.
 */
@Composable
internal fun PrefetchTilesAhead(scroll: LazyGridState, tiles: List<Pair<Any, TierListSummary>>, picturesOnWifiOnly: () -> Boolean) {
    val context = LocalContext.current
    val collages = LocalBoardCollages.current ?: if (LocalInspectionMode.current) null else BoardCollages.of(context)
    LaunchedEffect(tiles) {
        withFrameNanos { }
        val onScreen = scroll.layoutInfo.visibleItemsInfo.associateBy { it.key }
        val lastShown = tiles.indexOfLast { (key, _) -> key in onScreen }
        val measured = tiles.firstNotNullOfOrNull { (key, _) -> onScreen[key] } ?: return@LaunchedEffect
        val width = measured.size.width
        val ahead = tiles.drop(lastShown + 1).take(TILES_AHEAD).map { (_, list) -> list }
            .filterNot { list -> collages != null && list.collageKept(collages, width) }
            .map { it.art() }
        if (ahead.isEmpty()) return@LaunchedEffect
        prefetchArt(context, ahead, width, ceil(width / ART_RATIO).toInt(), picturesOnWifiOnly)
    }
}
