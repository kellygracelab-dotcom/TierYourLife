package com.artiuillab.tieryourlife.feature.tier.board.components.drag

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

/** How the finger that landed on a card made up its mind. */
private sealed interface Press {
    data class Lift(val at: Offset) : Press

    data object NotOurs : Press
}

/**
 * A drag that answers the finger at once. A finger that moves sideways lifts
 * the card straight away; one that moves up or down is scrolling the board
 * and is left alone; one that stays put lifts the card after the long-press
 * timeout. The stock long-press drag made every drag wait, and cancelled the
 * ones that started moving early.
 */
suspend fun PointerInputScope.detectTileDrag(
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val slop = viewConfiguration.touchSlop
    var travelled = Offset.Zero
    val decided = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull Press.NotOurs
            if (!change.pressed || change.isConsumed) return@withTimeoutOrNull Press.NotOurs
            travelled += change.positionChange()
            if (travelled.getDistance() > slop) {
                return@withTimeoutOrNull if (abs(travelled.x) > abs(travelled.y)) Press.Lift(change.position) else Press.NotOurs
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
