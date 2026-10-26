package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * A bar that leaves with the content under it and comes back on the first
 * scroll the other way, for a window too short to keep it: what it holds is
 * never more than one flick away, and the content gets the height while it is
 * read. The content always fills what the bar leaves, so a list under it is
 * measured once per frame the bar moves, not laid out under a hole.
 */
@Composable
fun ScrollAwayHeader(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var offset by remember { mutableFloatStateOf(0f) }
    // Written while measuring, read while scrolling: plain memory, not state,
    // so measuring does not ask to be measured again.
    val headerHeight = remember { IntArray(1) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val height = headerHeight[0]
                if (height == 0 || available.y == 0f) return Offset.Zero
                val before = offset
                offset = (offset + available.y).coerceIn(-height.toFloat(), 0f)
                return Offset(0f, offset - before)
            }
        }
    }

    Layout(
        contents = listOf(header, content),
        modifier = modifier
            .clipToBounds()
            .nestedScroll(connection),
    ) { (headerMeasurables, contentMeasurables), constraints ->
        val loose = constraints.copy(minHeight = 0)
        val headerPlaceables = headerMeasurables.map { it.measure(loose) }
        val height = headerPlaceables.maxOfOrNull { it.height } ?: 0
        headerHeight[0] = height
        val shift = offset.roundToInt().coerceIn(-height, 0)
        val top = height + shift
        val contentHeight = if (constraints.hasBoundedHeight) (constraints.maxHeight - top).coerceAtLeast(0) else Constraints.Infinity
        val contentConstraints = if (constraints.hasBoundedHeight) {
            constraints.copy(minHeight = contentHeight, maxHeight = contentHeight)
        } else {
            loose
        }
        val contentPlaceables = contentMeasurables.map { it.measure(contentConstraints) }
        val width = constraints.maxWidth
        val totalHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else top + (contentPlaceables.maxOfOrNull { it.height } ?: 0)
        layout(width, totalHeight) {
            contentPlaceables.forEach { it.place(0, top) }
            headerPlaceables.forEach { it.place(0, shift) }
        }
    }
}
