package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import kotlin.math.roundToInt

/**
 * The five ways Community lays itself out, decided by the window and never by
 * the device (§14): a cover, a short window, a phone held upright, and the two
 * widths that bring the rail.
 */
internal enum class HomeLayout(val feedColumns: Int, val coverHeight: Dp) {
    Cover(feedColumns = 1, coverHeight = 64.dp),
    CompactHeight(feedColumns = 4, coverHeight = CoverHeights.Landscape),
    Portrait(feedColumns = 2, coverHeight = CoverHeights.Phone),
    Medium(feedColumns = 3, coverHeight = CoverHeights.Phone),
    Wide(feedColumns = 4, coverHeight = CoverHeights.Tablet),
}

/** Below this height only one bar may stand above the content, and it scrolls away. */
internal val CompactHeightBelow = 480.dp

/** Wide windows centre their content rather than stretch it (§7.1). */
internal val WideContentMax = 1080.dp

internal fun homeLayoutFor(shape: WindowShape, height: Dp): HomeLayout = when {
    shape == WindowShape.Cover -> HomeLayout.Cover
    height < CompactHeightBelow -> HomeLayout.CompactHeight
    shape == WindowShape.Wide -> HomeLayout.Wide
    shape == WindowShape.Medium -> HomeLayout.Medium
    else -> HomeLayout.Portrait
}

/** Read inside the nested scroll and the layout alike; a plain holder, since the layout writes it while measuring. */
private class HeaderOffset {
    var height = 0
    var offset = 0f
}

/**
 * A header that leaves with the first scroll down and comes back with the
 * first scroll up, wherever the content is (enterAlways). The body shrinks
 * and grows with it, so nothing under the header is ever covered.
 */
@Composable
internal fun EnterAlwaysColumn(
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = remember { HeaderOffset() }
    val redraw = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val height = state.height.toFloat()
                if (height == 0f) return Offset.Zero
                val before = state.offset
                state.offset = (state.offset + available.y).coerceIn(-height, 0f)
                redraw.floatValue = state.offset
                return Offset(0f, state.offset - before)
            }
        }
    }
    Layout(
        content = {
            Box { header() }
            Box { body() }
        },
        modifier = modifier
            .nestedScroll(connection)
            .clipToBounds(),
    ) { measurables, constraints ->
        // Read so a scroll relayouts.
        redraw.floatValue
        val headerPlaceable = measurables[0].measure(constraints.copy(minHeight = 0))
        state.height = headerPlaceable.height
        state.offset = state.offset.coerceIn(-headerPlaceable.height.toFloat(), 0f)
        val shift = state.offset.roundToInt()
        val visible = headerPlaceable.height + shift
        val bodyHeight = (constraints.maxHeight - visible).coerceAtLeast(0)
        val bodyPlaceable = measurables[1].measure(constraints.copy(minHeight = bodyHeight, maxHeight = bodyHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceable.place(0, shift)
            bodyPlaceable.place(0, visible)
        }
    }
}
