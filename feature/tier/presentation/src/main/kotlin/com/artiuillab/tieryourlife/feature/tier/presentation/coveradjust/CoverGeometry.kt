package com.artiuillab.tieryourlife.feature.tier.presentation.coveradjust

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import com.artiuillab.tieryourlife.feature.tier.domain.model.coverFrame

/** Whether the picture has told us its size, the one thing the formula needs. */
internal val Size.isKnown: Boolean get() = isSpecified && width > 0f && height > 0f

/**
 * [painter] drawn so [frame] shows exactly the formula's crop for [focus].
 * Nothing clips it: the editor shows the rest of the picture around the frame.
 */
internal fun DrawScope.drawCover(painter: Painter, focus: CoverFocus, frame: Rect) {
    val picture = painter.intrinsicSize
    if (!picture.isKnown || frame.width <= 0f || frame.height <= 0f) return
    val crop = coverFrame(picture.width, picture.height, frame.width / frame.height, focus)
    val scale = frame.width / crop.width.toFloat()
    translate(frame.left - crop.left.toFloat() * scale, frame.top - crop.top.toFloat() * scale) {
        scale(scale, scale, pivot = Offset.Zero) {
            with(painter) { draw(picture) }
        }
    }
}

/**
 * The focus after a finger moved the picture by [pan] screen pixels and
 * pinched it by [zoomBy], in a [frame] showing a [picture]. The picture moves
 * with the finger, so the focus moves against it; it stops where the frame
 * meets an edge, rather than going on moving a point nothing shows.
 */
internal fun CoverFocus.moved(pan: Offset, zoomBy: Float, picture: Size, frame: Size): CoverFocus {
    if (!picture.isKnown || !frame.isKnown) return this
    val zoomed = copy(zoom = (zoom * zoomBy).coerceIn(CoverFocus.MIN_ZOOM, CoverFocus.MAX_ZOOM))
    val crop = coverFrame(picture.width, picture.height, frame.width / frame.height, zoomed)
    val scale = frame.width / crop.width
    val centreX = (crop.left + crop.width / 2 - pan.x / scale).coerceIn(crop.width / 2, picture.width - crop.width / 2)
    val centreY = (crop.top + crop.height / 2 - pan.y / scale).coerceIn(crop.height / 2, picture.height - crop.height / 2)
    return zoomed.copy(x = (centreX / picture.width).toFloat(), y = (centreY / picture.height).toFloat())
}
