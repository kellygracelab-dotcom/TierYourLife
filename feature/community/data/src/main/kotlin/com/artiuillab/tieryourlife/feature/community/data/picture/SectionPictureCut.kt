package com.artiuillab.tieryourlife.feature.community.data.picture

import com.artiuillab.tieryourlife.feature.community.domain.model.PictureCrop
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionPictureSize
import kotlin.math.roundToInt

/** A rectangle in a decoded bitmap's pixels, and the size it is exported at. */
internal data class PixelCut(val left: Int, val top: Int, val width: Int, val height: Int, val exportWidth: Int, val exportHeight: Int)

/**
 * Where [crop] falls in a bitmap of [width] by [height], held inside it and
 * at 16:10, and what it is exported as: 1600 × 1000 at most, as wide as the
 * cut below that, and never below 960 × 600 — a cut that small is null.
 * [scale] is how far the bitmap was shrunk while decoding, so the floor is
 * measured on the picture itself.
 */
internal fun sectionCut(width: Int, height: Int, crop: PictureCrop, scale: Int = 1): PixelCut? {
    val cutWidth = minOf((crop.width * width).roundToInt(), (height * SectionPictureSize.ASPECT).roundToInt(), width).coerceAtLeast(1)
    val cutHeight = (cutWidth / SectionPictureSize.ASPECT).roundToInt().coerceIn(1, height)
    val exportWidth = SectionPictureSize.exportWidth(cutWidth * scale) ?: return null
    val left = (crop.left * width).roundToInt().coerceIn(0, width - cutWidth)
    val top = (crop.top * height).roundToInt().coerceIn(0, height - cutHeight)
    return PixelCut(left, top, cutWidth, cutHeight, exportWidth, (exportWidth / SectionPictureSize.ASPECT).roundToInt())
}

/** How far to shrink while decoding so the cut still holds the widest export. */
internal fun sectionSampleSize(width: Int, crop: PictureCrop): Int {
    val cutWidth = width * crop.width
    var sample = 1
    while (cutWidth / (sample * 2) >= SectionPictureSize.MAX_WIDTH) {
        sample *= 2
    }
    return sample
}
