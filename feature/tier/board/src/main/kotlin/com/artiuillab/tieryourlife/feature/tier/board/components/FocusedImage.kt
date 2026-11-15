package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A cover picture cut to whatever frame it is given by the one formula every
 * platform uses ([com.artiuillab.tieryourlife.feature.tier.domain.model.coverFrame]):
 * the chosen point in the middle, as far in as chosen, never past an edge.
 * Without a focus it is the plain centre crop it always was.
 */
@Composable
fun FocusedImage(
    model: Any?,
    focus: CoverFocus?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    crossfadeMillis: Int = 0,
) {
    BoxWithConstraints(modifier) {
        val zoom = focus?.zoom ?: CoverFocus.MIN_ZOOM
        val context = LocalContext.current
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val request = remember(model, zoom, width, height, crossfadeMillis) {
            ImageRequest.Builder(context)
                .data(model)
                .apply {
                    // Decoded as large as the zoom shows it, or a close crop would be a blur.
                    if (zoom > CoverFocus.MIN_ZOOM && constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
                        size(ceil(width * zoom).toInt(), ceil(height * zoom).toInt())
                        scale(Scale.FILL)
                    }
                    if (crossfadeMillis > 0) crossfade(crossfadeMillis)
                }
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = focusScale(focus),
            alignment = focusAlignment(focus),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** The largest crop for the frame, [CoverFocus.zoom] times closer. */
fun focusScale(focus: CoverFocus?): ContentScale = object : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        val fill = max(dstSize.width / srcSize.width, dstSize.height / srcSize.height) * (focus?.zoom ?: CoverFocus.MIN_ZOOM)
        return ScaleFactor(fill, fill)
    }
}

/** The focus point in the middle of the frame, held back from showing past an edge. */
fun focusAlignment(focus: CoverFocus?): Alignment = Alignment { size, space, _: LayoutDirection ->
    val (x, y) = focus ?: CoverFocus.CENTRED
    IntOffset(
        x = -(x * size.width - space.width / 2f).coerceIn(0f, max(0, size.width - space.width).toFloat()).roundToInt(),
        y = -(y * size.height - space.height / 2f).coerceIn(0f, max(0, size.height - space.height).toFloat()).roundToInt(),
    )
}
