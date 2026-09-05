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
 * How wide content may get. Filling a tablet's width is not using it: a row
 * 1500dp across puts a label at one edge and its switch at the other. Below
 * these numbers nothing changes, so a phone sees none of this.
 */
object ContentWidth {

    /**
     * Rows where a label and the control it belongs to have to stay within one
     * glance of each other: settings, a list of boards, a queue of reports.
     */
    val Reading: Dp = 640.dp

    /**
     * A board: a tier is a horizontal strip of cards, and width is the one
     * thing it can spend. Past this the band and the last card stop reading
     * as one row.
     */
    val Board: Dp = 1080.dp

    /** A back-and-forth of prompts and answers, which reads like a chat. */
    val Conversation: Dp = 720.dp

    /** A sentence or two with nothing else on screen. Line length is the limit. */
    val Message: Dp = 360.dp
}

/** Capped and centred together: a capped column pinned to a tablet's left edge is the same problem in a different shape. */
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

/** The same cap for something that places itself; centring is the caller's. */
fun Modifier.atMost(max: Dp): Modifier = widthIn(max = max).fillMaxWidth()
