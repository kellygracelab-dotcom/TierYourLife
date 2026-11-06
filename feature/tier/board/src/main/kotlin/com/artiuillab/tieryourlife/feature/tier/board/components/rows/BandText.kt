package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import kotlin.math.max

const val BAND_LABEL_MAX_LINES = 3
private const val CAPTION_MAX_LINES = 2
private const val SMALLEST_LABEL_SP = 10f
private const val LABEL_STEP_SP = 1f

/**
 * A band's name and caption. A letter stays as it always was; a name too long
 * for the band breaks between words and gets smaller until it fits, and
 * [height] too where the row does not grow. [width] is the band's own and
 * [padding] what it keeps on each side: they round to pixels apart, as the
 * layout rounds them, and one pixel short is enough to split a word.
 */
@Composable
internal fun BandText(
    label: String,
    caption: String?,
    labelStyle: TextStyle,
    captionStyle: TextStyle,
    color: Color,
    width: Dp,
    padding: Dp,
    height: Dp? = null,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = LocalTextStyle.current
    val shownCaption = caption?.takeIf { density.fontScale < CAPTION_HIDDEN_FONT_SCALE }
    val fitted = remember(label, shownCaption, labelStyle, captionStyle, base, width, padding, height, density) {
        val widthPx = with(density) { width.roundToPx() - padding.roundToPx() * 2 }
        val captionPx = shownCaption?.let {
            measurer.measure(it, base.merge(captionStyle), TextOverflow.Ellipsis, maxLines = CAPTION_MAX_LINES, constraints = Constraints(maxWidth = widthPx)).size.height
        } ?: 0
        val heightPx = height?.let { with(density) { it.toPx().toInt() } - captionPx }
        fittedBandLabel(measurer, label, base.merge(labelStyle), widthPx, heightPx)
    }
    Text(
        text = label,
        style = fitted,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = BAND_LABEL_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
    if (shownCaption != null) {
        Text(
            text = shownCaption,
            maxLines = CAPTION_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = captionStyle,
            color = color.copy(alpha = 0.7f),
        )
    }
}

/** The largest size, down to a floor, at which [label] fits without a word broken in two. */
internal fun fittedBandLabel(measurer: TextMeasurer, label: String, style: TextStyle, widthPx: Int, heightPx: Int?): TextStyle {
    val largest = style.fontSize
    if (largest.type != TextUnitType.Sp || widthPx <= 0) return style
    val lineRatio = if (style.lineHeight.type == TextUnitType.Sp) style.lineHeight.value / largest.value else null
    var size = largest.value
    while (true) {
        val candidate = style.copy(fontSize = size.sp, lineHeight = lineRatio?.let { (size * it).sp } ?: style.lineHeight)
        if (size <= SMALLEST_LABEL_SP || fits(measurer, label, candidate, widthPx, heightPx)) return candidate
        size = max(SMALLEST_LABEL_SP, size - LABEL_STEP_SP)
    }
}

private fun fits(measurer: TextMeasurer, label: String, style: TextStyle, widthPx: Int, heightPx: Int?): Boolean {
    val layout = measurer.measure(label, style, TextOverflow.Clip, maxLines = BAND_LABEL_MAX_LINES, constraints = Constraints(maxWidth = widthPx))
    if (layout.hasVisualOverflow) return false
    if (heightPx != null && layout.size.height > heightPx) return false
    return (0 until layout.lineCount - 1).all { line -> breaksBetweenWords(label, layout.getLineEnd(line)) }
}

private fun breaksBetweenWords(text: String, end: Int): Boolean =
    end !in 1 until text.length || text[end - 1].isWhitespace() || text[end].isWhitespace() || text[end - 1] == '-'
