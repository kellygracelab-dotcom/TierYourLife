package com.artiuillab.tieryourlife.core.theme.layout

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt

/**
 * A bar that leaves with the content under it and comes back on the first
 * scroll the other way, for a window too short to keep it: what it holds is
 * never more than one flick away, and the content gets the height while it is
 * read. The content always fills what the bar leaves, so a list under it is
 * measured once per frame the bar moves, not laid out under a hole.
 *
 * [pinned] holds the bar in place, brought back if it had left, for as long
 * as what it holds is being used (a field being typed into, a count being
 * acted on). The layout stays the same, so the content keeps its scroll.
 */
@Composable
fun ScrollAwayHeader(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    content: @Composable () -> Unit,
) {
    var offset by remember { mutableFloatStateOf(0f) }
    val held by rememberUpdatedState(pinned)
    LaunchedEffect(pinned) {
        if (pinned && offset != 0f) {
            animate(initialValue = offset, targetValue = 0f, animationSpec = spring()) { value, _ -> offset = value }
        }
    }
    // Written while measuring, read while scrolling: plain memory, not state,
    // so measuring does not ask to be measured again.
    val headerHeight = remember { IntArray(1) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val height = headerHeight[0]
                if (held || height == 0 || available.y == 0f) return Offset.Zero
                val before = offset
                offset = (offset + available.y).coerceIn(-height.toFloat(), 0f)
                return Offset(0f, offset - before)
            }

            // A bar left half out once the finger lifts reads as a glitch;
            // it settles to whichever side it is nearer.
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val height = headerHeight[0].toFloat()
                if (held) return Velocity.Zero
                if (height == 0f || offset == 0f || offset == -height) return Velocity.Zero
                val target = if (offset < -height / 2) -height else 0f
                animate(initialValue = offset, targetValue = target, animationSpec = spring()) { value, _ -> offset = value }
                return Velocity.Zero
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
