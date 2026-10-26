package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The two heights a screen's bar comes in, and how far its first and last buttons reach past the content edge. */
object TopBarDefaults {

    /** A window under [WindowShape.ShortBelow]: one slim bar that leaves with the content. */
    val CompactHeight: Dp = 48.dp

    /** The app's bars have stood at 56 since the start; a short window is the only place they shrink. */
    val Height: Dp = 56.dp

    /** The side padding of the content the bar stands over; the bar uses the same so its title starts where the content does. */
    val ContentPadding: Dp = 16.dp

    /**
     * Half a 48dp touch target minus half a 24dp glyph: the button reaches
     * this far past the column so the glyph itself lines up with the text below.
     */
    val Overhang: Dp = 12.dp
}

/**
 * A screen's bar, standing on the same column as the content under it: the
 * title starts where the content starts and the last button ends where it
 * ends, while the bar's background still spans the window. A bar laid out
 * against the window's edges put the title in the middle of a wide window
 * and the buttons a window away from it.
 *
 * [count] follows the title on the same line in a short window, where there
 * is no room for a line of its own; it gives way before the title does.
 * [inline] follows them both: what a taller window would put on a row of
 * its own under the bar, such as a screen's chips.
 */
@Composable
fun ColumnAlignedTopBar(
    title: String,
    modifier: Modifier = Modifier,
    contentMax: Dp = Dp.Infinity,
    contentPad: Dp = TopBarDefaults.ContentPadding,
    compact: Boolean = isShortWindow,
    navigation: (@Composable () -> Unit)? = null,
    count: String? = null,
    titleTestTag: String? = null,
    divider: Boolean = false,
    inline: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Row(
                modifier = Modifier
                    .widthIn(max = contentMax)
                    .fillMaxWidth()
                    .padding(horizontal = contentPad)
                    .height(if (compact) TopBarDefaults.CompactHeight else TopBarDefaults.Height),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (navigation != null) {
                    Box(Modifier.overhang(start = true)) { navigation() }
                    Spacer(Modifier.width(4.dp))
                }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    // Title and count share a baseline in a row of their own, centred
                    // like everything else: taller neighbours must not lift them.
                    Row(Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            modifier = Modifier
                                .alignByBaseline()
                                .let { if (titleTestTag != null) it.testTag(titleTestTag) else it },
                            style = if (compact) CompactTitleStyle else MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (count != null) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = count,
                                modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                                style = CountStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (inline != null) {
                        Spacer(Modifier.width(12.dp))
                        inline()
                    }
                }
                Row(Modifier.overhang(start = false), verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * A screen with a bar over scrolling content. In a short window the bar
 * leaves with the content and comes back on the first scroll the other way,
 * so the content gets the height; anywhere else it stays. The status bar's
 * strip stays put either way, so nothing scrolls under the clock.
 * [pinned] holds the short window's bar in place without changing the
 * layout, so whatever scrolls under it keeps its place.
 */
@Composable
fun BarredScreen(
    bar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = isShortWindow,
    pinned: Boolean = false,
    content: @Composable () -> Unit,
) {
    val outer = modifier.fillMaxSize().statusBarsPadding()
    if (compact) {
        ScrollAwayHeader(header = bar, modifier = outer, pinned = pinned, content = content)
    } else {
        Column(outer) {
            bar()
            Box(Modifier.fillMaxWidth().weight(1f)) { content() }
        }
    }
}

/** For a screen's own bar row: [TopBarDefaults.CompactHeight] in a short window, [TopBarDefaults.Height] otherwise. */
@Composable
fun topBarHeight(): Dp = if (isShortWindow) TopBarDefaults.CompactHeight else TopBarDefaults.Height

/** The bar title's type: smaller in a short window, where the bar is. */
@Composable
fun topBarTitleStyle(): TextStyle = if (isShortWindow) CompactTitleStyle else MaterialTheme.typography.titleLarge

private val CompactTitleStyle = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
private val CountStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)

/**
 * Lays the button out [TopBarDefaults.Overhang] narrower than it measures and
 * lets it reach past the column by that much, so its glyph, not its touch
 * target, lines up with the content edge.
 */
internal fun Modifier.overhang(start: Boolean): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val reach = TopBarDefaults.Overhang.roundToPx()
    if (placeable.width == 0) {
        layout(0, placeable.height) {}
    } else {
        val width = (placeable.width - reach).coerceAtLeast(0)
        layout(width, placeable.height) {
            placeable.placeRelative(if (start) -reach else 0, 0)
        }
    }
}
