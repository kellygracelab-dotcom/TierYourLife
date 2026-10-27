package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import kotlin.math.roundToInt

/**
 * The chosen square of a whole photo, for the moment between Done and the
 * cut picture being ready: the same fractions the phone cuts by, so nothing
 * moves when the cut one takes its place.
 */
internal class FaceCropTransformation(private val crop: FaceCrop) : Transformation() {

    override val cacheKey: String = "face-crop-${crop.left}-${crop.top}-${crop.width}-${crop.height}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val square = squareIn(input.width, input.height, crop)
        return Bitmap.createBitmap(input, square.left, square.top, square.side, square.side)
    }
}

internal data class CropSquare(val left: Int, val top: Int, val side: Int)

/** Where [crop] falls in a picture of [width] by [height], always a square that fits. */
internal fun squareIn(width: Int, height: Int, crop: FaceCrop): CropSquare {
    val shorter = minOf(width, height)
    val side = minOf(crop.width * width, crop.height * height).roundToInt().coerceIn(1, shorter)
    val left = (crop.left * width).roundToInt().coerceIn(0, width - side)
    val top = (crop.top * height).roundToInt().coerceIn(0, height - side)
    return CropSquare(left, top, side)
}
