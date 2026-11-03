package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/** Which ways the cards around a card scroll: a finger moving that way is scrolling them, not lifting the card. */
enum class TileScroll {
    /** A grid or a wrapping row on the board: sideways lifts at once, and so does a short hold. */
    UpAndDown,

    /** The pool's strip under the board: up or down lifts at once, and so does a short hold. */
    Sideways,

    /** A sideways strip on a board that scrolls up and down: only a short hold lifts. */
    BothWays,
}

internal fun TileScroll.liftAxis(): Orientation? = when (this) {
    TileScroll.UpAndDown -> Orientation.Horizontal
    TileScroll.Sideways -> Orientation.Vertical
    TileScroll.BothWays -> null
}

/**
 * Inside a sideways strip the hold waits long enough for a swipe that starts
 * slowly to leave the touch slop first; the system's long press (half a second)
 * felt like the card was stuck.
 */
internal fun TileScroll.holdMillis(): Long = when (this) {
    TileScroll.UpAndDown -> DRAG_LONG_PRESS_TIMEOUT_MILLIS
    TileScroll.Sideways, TileScroll.BothWays -> STRIP_HOLD_MILLIS
}

private const val STRIP_HOLD_MILLIS = 200L

/** How the finger that landed on a card made up its mind. */
private sealed interface Press {
    data class Lift(val at: Offset) : Press

    data object NotOurs : Press
}

/**
 * A drag that answers the finger at once. A finger that moves along [liftAxis]
 * lifts the card straight away; one that moves the other way is scrolling and
 * is left alone; one that stays put lifts the card after [holdMillis]. With no
 * [liftAxis] every move is scrolling and only the hold lifts.
 */
suspend fun PointerInputScope.detectTileDrag(
    liftAxis: Orientation?,
    holdMillis: Long,
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val slop = viewConfiguration.touchSlop
    var travelled = Offset.Zero
    val decided = withTimeoutOrNull(holdMillis) {
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull Press.NotOurs
            if (!change.pressed || change.isConsumed) return@withTimeoutOrNull Press.NotOurs
            travelled += change.positionChange()
            if (travelled.getDistance() > slop) {
                if (!travelled.leansTowards(liftAxis)) return@withTimeoutOrNull Press.NotOurs
                change.consume()
                return@withTimeoutOrNull Press.Lift(change.position)
            }
        }
        @Suppress("UNREACHABLE_CODE")
        Press.NotOurs
    } ?: Press.Lift(down.position)
    val start = (decided as? Press.Lift)?.at ?: return@awaitEachGesture

    onDragStart(start)
    val finished = drag(down.id) { change ->
        onDrag(change, change.positionChange())
        change.consume()
    }
    if (finished) onDragEnd() else onDragCancel()
}

private fun Offset.leansTowards(axis: Orientation?): Boolean = when (axis) {
    Orientation.Horizontal -> abs(x) > abs(y)
    Orientation.Vertical -> abs(y) > abs(x)
    null -> false
}

/**
 * A double tap that fires on the second finger-down rather than its release:
 * the sheet is already opening when the finger lifts.
 */
suspend fun PointerInputScope.detectDoubleTapOnPress(onDoubleTap: () -> Unit) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false)
    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation() } ?: return@awaitEachGesture
    val second = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
        awaitFirstDown(requireUnconsumed = false)
    } ?: return@awaitEachGesture
    second.consume()
    onDoubleTap()
}
