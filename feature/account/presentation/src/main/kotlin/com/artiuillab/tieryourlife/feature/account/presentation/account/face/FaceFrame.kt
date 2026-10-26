package com.artiuillab.tieryourlife.feature.account.presentation.account.face

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop

internal const val MAX_FACE_ZOOM = 5f

/**
 * Where a picture sits behind the square frame, in the frame's own units:
 * the frame is 1 wide, the picture first covers it exactly ([zoom] 1, its
 * shorter edge equal to the frame's) and is then zoomed and moved by
 * [offsetX], [offsetY] from the middle. Kept in these units so turning the
 * phone, which changes the frame's size in pixels, keeps the same face.
 */
internal data class FaceFrame(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    /** Never touched: the middle square, which needs no cutting of its own. */
    val untouched: Boolean get() = zoom == 1f && offsetX == 0f && offsetY == 0f

    /**
     * Moved by a gesture, in frame units, and zoomed by [zoomBy]; then held so
     * the picture still covers the whole frame. [aspect] is the picture's
     * width over its height.
     */
    fun moved(panX: Float, panY: Float, zoomBy: Float, aspect: Float): FaceFrame {
        val nextZoom = (zoom * zoomBy).coerceIn(1f, MAX_FACE_ZOOM)
        val (width, height) = shownSize(aspect, nextZoom)
        val reachX = (width - 1f) / 2f
        val reachY = (height - 1f) / 2f
        // Plus nought: a clamp can answer -0, and a frame put back in the middle should equal a fresh one.
        return FaceFrame(
            zoom = nextZoom,
            offsetX = (offsetX + panX).coerceIn(-reachX, reachX) + 0f,
            offsetY = (offsetY + panY).coerceIn(-reachY, reachY) + 0f,
        )
    }

    /** The square inside the frame, as fractions of the picture's width and height. */
    fun crop(aspect: Float): FaceCrop {
        val (width, height) = shownSize(aspect, zoom)
        val cropWidth = 1f / width
        val cropHeight = 1f / height
        return FaceCrop(
            left = 0.5f - offsetX / width - cropWidth / 2f,
            top = 0.5f - offsetY / height - cropHeight / 2f,
            width = cropWidth,
            height = cropHeight,
        )
    }

    companion object {
        val Saver: Saver<FaceFrame, Any> = listSaver(
            save = { listOf(it.zoom, it.offsetX, it.offsetY) },
            restore = { FaceFrame(it[0], it[1], it[2]) },
        )
    }
}

/** The picture's size behind the frame, in frame units. */
private fun shownSize(aspect: Float, zoom: Float): Pair<Float, Float> =
    if (aspect >= 1f) aspect * zoom to zoom else zoom to zoom / aspect
