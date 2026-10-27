package com.artiuillab.tieryourlife.feature.account.data.picture

import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import kotlin.math.roundToInt

/** A square in a decoded bitmap's pixels. */
internal data class PixelSquare(val left: Int, val top: Int, val side: Int)

/**
 * Where [crop] falls in a bitmap of [width] by [height]: the middle square
 * when there is no crop, and always a square that fits, whatever rounding
 * or a stale crop would otherwise leave hanging over an edge.
 */
internal fun faceSquare(width: Int, height: Int, crop: FaceCrop?): PixelSquare {
    val shorter = minOf(width, height)
    if (crop == null) return PixelSquare((width - shorter) / 2, (height - shorter) / 2, shorter)
    val side = minOf(crop.width * width, crop.height * height).roundToInt().coerceIn(1, shorter)
    val left = (crop.left * width).roundToInt().coerceIn(0, width - side)
    val top = (crop.top * height).roundToInt().coerceIn(0, height - side)
    return PixelSquare(left, top, side)
}

/** How far to shrink while decoding so the chosen square still covers the face. */
internal fun faceSampleSize(width: Int, height: Int, crop: FaceCrop? = null): Int {
    val side = if (crop == null) minOf(width, height) else minOf(width * crop.width, height * crop.height).toInt()
    var sample = 1
    while (side / (sample * 2) >= FACE_EDGE_PX) {
        sample *= 2
    }
    return sample
}
