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
import kotlin.math.min

/** Whether the picture has told us its size, the one thing the formula needs. */
internal val Size.isKnown: Boolean get() = isSpecified && width > 0f && height > 0f

/**
 * [painter] drawn so [frame] shows exactly the formula's crop for [focus].
 * Nothing clips it: a caller that wants only the frame clips it itself.
 */
internal fun DrawScope.drawCover(painter: Painter, focus: CoverFocus?, frame: Rect) {
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

/** The whole [picture] as large as fits in [space], in its middle; empty until the picture is known. */
internal fun fitted(picture: Size, space: Size): Rect {
    if (!picture.isKnown || !space.isKnown) return Rect.Zero
    val scale = min(space.width / picture.width, space.height / picture.height)
    val size = Size(picture.width * scale, picture.height * scale)
    return Rect(Offset((space.width - size.width) / 2, (space.height - size.height) / 2), size)
}

/** Where a frame of [aspect] falls on a [picture] drawn at [placed]: the formula's crop, in the same pixels. */
internal fun frameOn(placed: Rect, picture: Size, aspect: Float, focus: CoverFocus?): Rect {
    if (!picture.isKnown || placed.isEmpty) return Rect.Zero
    val crop = coverFrame(picture.width, picture.height, aspect, focus)
    val scale = placed.width / picture.width
    return Rect(
        Offset(placed.left + crop.left.toFloat() * scale, placed.top + crop.top.toFloat() * scale),
        Size(crop.width.toFloat() * scale, crop.height.toFloat() * scale),
    )
}

/**
 * The focus after a finger moved by [pan] screen pixels over the whole
 * picture drawn at [placed] and pinched by [zoomBy]. The point follows the
 * finger anywhere on the picture, even where the widest frame already meets an
 * edge: a narrower tile still has room to move.
 */
internal fun CoverFocus.moved(pan: Offset, zoomBy: Float, placed: Rect): CoverFocus {
    if (placed.isEmpty) return this
    return CoverFocus(
        x = (x + pan.x / placed.width).coerceIn(0f, 1f),
        y = (y + pan.y / placed.height).coerceIn(0f, 1f),
        zoom = (zoom * zoomBy).coerceIn(CoverFocus.MIN_ZOOM, CoverFocus.MAX_ZOOM),
    )
}

/** The focus put where a tap landed on the picture drawn at [placed], keeping the zoom. */
internal fun CoverFocus.placedAt(tap: Offset, placed: Rect): CoverFocus {
    if (placed.isEmpty) return this
    return copy(
        x = ((tap.x - placed.left) / placed.width).coerceIn(0f, 1f),
        y = ((tap.y - placed.top) / placed.height).coerceIn(0f, 1f),
    )
}
