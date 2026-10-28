package com.artiuillab.tieryourlife.feature.community.presentation.propose

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureCrop
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionPictureSize
import com.artiuillab.tieryourlife.feature.community.domain.repository.PictureSize

/** The 16:10 frame is 1 wide and this tall, in its own units. */
private const val FRAME_HEIGHT = 1f / SectionPictureSize.ASPECT

/** The most the picture may be zoomed whatever its size: past this a tile is all texture. */
internal const val MAX_PICTURE_ZOOM = 4f

/**
 * Where a picture sits behind the 16:10 frame, in the frame's own units, as
 * the face's square frame does it (account's FaceFrame): the frame is 1 wide,
 * the picture first covers it exactly ([zoom] 1) and is then zoomed and moved
 * by [offsetX], [offsetY] from the middle. Turning the phone keeps the crop.
 */
data class PictureFrame(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    /**
     * Moved by a gesture and zoomed by [zoomBy], then held so the picture still
     * covers the frame and the crop stays at least 960 px wide.
     */
    fun moved(panX: Float, panY: Float, zoomBy: Float, picture: PictureSize): PictureFrame {
        val nextZoom = (zoom * zoomBy).coerceIn(1f, maxZoom(picture))
        val (width, height) = shownSize(picture.aspect, nextZoom)
        val reachX = (width - 1f) / 2f
        val reachY = (height - FRAME_HEIGHT) / 2f
        // Plus nought: a clamp can answer -0, and a frame put back in the middle should equal a fresh one.
        return PictureFrame(
            zoom = nextZoom,
            offsetX = (offsetX + panX).coerceIn(-reachX, reachX) + 0f,
            offsetY = (offsetY + panY).coerceIn(-reachY, reachY) + 0f,
        )
    }

    /** What is inside the frame, as fractions of the picture's width and height. */
    fun crop(picture: PictureSize): PictureCrop {
        val (width, height) = shownSize(picture.aspect, zoom)
        val cropWidth = 1f / width
        val cropHeight = FRAME_HEIGHT / height
        return PictureCrop(
            left = 0.5f - offsetX / width - cropWidth / 2f,
            top = 0.5f - offsetY / height - cropHeight / 2f,
            width = cropWidth,
            height = cropHeight,
        )
    }

    companion object {
        val Saver: Saver<PictureFrame, Any> = listSaver(
            save = { listOf(it.zoom, it.offsetX, it.offsetY) },
            restore = { PictureFrame(it[0], it[1], it[2]) },
        )

        /** The widest 16:10 cut the picture holds, in its own pixels: what decides whether it is big enough. */
        fun widestCut(picture: PictureSize): Int =
            if (picture.aspect >= SectionPictureSize.ASPECT) (picture.height * SectionPictureSize.ASPECT).toInt() else picture.width

        /** As far as the cut can shrink and stay 960 px wide, and never past [MAX_PICTURE_ZOOM]. */
        fun maxZoom(picture: PictureSize): Float =
            (widestCut(picture).toFloat() / SectionPictureSize.MIN_WIDTH).coerceIn(1f, MAX_PICTURE_ZOOM)
    }
}

private val PictureSize.aspect: Float get() = width.toFloat() / height.coerceAtLeast(1)

/** The picture's size behind the frame, in frame units. */
private fun shownSize(aspect: Float, zoom: Float): Pair<Float, Float> =
    if (aspect >= SectionPictureSize.ASPECT) FRAME_HEIGHT * aspect * zoom to FRAME_HEIGHT * zoom else zoom to zoom / aspect
