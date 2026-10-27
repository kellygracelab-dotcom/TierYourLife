package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The name on a card without a picture, broken only between words. Hyphenation
 * alone did not stop "Ex Machina" becoming "Machin / a" on a narrow card: a
 * proper noun has no hyphenation point, so the line breaker cut it anywhere.
 * Here the type steps down until every word fits on one line, never below
 * [smallest]; only a word too long even then is left to the breaker.
 */
@Composable
fun CardLabel(
    text: String,
    style: TextStyle,
    color: Color,
    maxLines: Int,
    modifier: Modifier = Modifier,
    smallest: TextUnit = 6.sp,
) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val width = constraints.maxWidth
        val fitted = remember(text, style, width) {
            val words = text.split(' ').filter { it.isNotBlank() }
            if (words.isEmpty() || !style.fontSize.isSp) return@remember style
            fun at(size: Float): TextStyle = style.copy(
                fontSize = size.sp,
                lineHeight = if (style.lineHeight.isSp) (style.lineHeight.value * size / style.fontSize.value).sp else style.lineHeight,
            )
            fun fits(candidate: TextStyle): Boolean = words.all { word ->
                measurer.measure(word, candidate, softWrap = false, maxLines = 1).size.width <= width
            }
            // Down in half-points until every word sits on one line: a ratio
            // alone landed a hair too wide and the breaker still cut the word.
            var size = style.fontSize.value
            while (size > smallest.value && !fits(at(size))) size -= STEP
            at(size.coerceAtLeast(smallest.value))
        }
        Text(
            text = text,
            style = fitted.copy(hyphens = Hyphens.None, lineBreak = LineBreak.Paragraph),
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private const val STEP = 0.5f
