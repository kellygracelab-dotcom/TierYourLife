package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How wide content is allowed to get.
 *
 * A phone answers this by being narrow. A tablet does not, and filling its
 * width is not the same as using it: a row 1500dp across puts a label at one
 * edge and the switch it belongs to at the other, and no one can hold the two
 * together. Content gets a measure, and the window keeps the rest.
 *
 * The numbers are the widest a kind of content stays readable at, not the
 * width it always takes. Below them nothing changes, which is why a phone sees
 * none of this.
 */
object ContentWidth {

    /**
     * Rows where a label and the control it belongs to have to stay within one
     * glance of each other: settings, a list of boards, a queue of reports.
     */
    val Reading: Dp = 640.dp

    /**
     * A board. Wider than everything else on purpose -- a tier is a horizontal
     * strip of cards, and width is the one thing it can spend. Past this the
     * band of colour on the left and the last card on the right stop being
     * read as one row.
     */
    val Board: Dp = 1080.dp

    /** A back-and-forth of prompts and answers, which reads like a chat. */
    val Conversation: Dp = 720.dp

    /** A sentence or two with nothing else on screen. Line length is the limit. */
    val Message: Dp = 360.dp
}

/**
 * Content at its own measure, centred, with the window keeping the rest.
 *
 * Take this rather than `widthIn` at a call site: it also centres, and the two
 * belong together -- a capped column pinned to the left edge of a tablet is
 * the same problem in a different shape.
 */
@Composable
fun CenteredContent(
    max: Dp,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = max).fillMaxWidth(),
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

/**
 * The same cap for something that places itself -- a bar, a sheet, a row that
 * already has its own container. Centring is the caller's, because these are
 * usually inside a Column or Box that is already doing it.
 */
fun Modifier.atMost(max: Dp): Modifier = widthIn(max = max).fillMaxWidth()
