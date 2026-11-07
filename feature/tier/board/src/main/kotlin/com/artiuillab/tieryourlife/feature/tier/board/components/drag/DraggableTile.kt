package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.ItemTile
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.LocalTextCardRoom
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import kotlin.math.roundToInt

private val DETAIL_KEYS = setOf(Key.Enter, Key.DirectionCenter, Key.Spacebar)

@Composable
fun DraggableTile(
    item: TierItem,
    sourceTierId: Long,
    width: Dp,
    height: Dp,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onPositioned: (Rect) -> Unit = {},
    onDoubleTap: (itemId: Long) -> Unit = {},
    /** True while the row draws the card's slot elsewhere: the lifted card's own place then takes no room. */
    collapseWhileLifted: Boolean = false,
    scroll: TileScroll = TileScroll.UpAndDown,
) {
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var sizePx by remember { mutableStateOf(Offset.Zero) }
    val isDragging = dragController.draggedPayload?.itemId == item.id ||
        dragController.returning?.payload?.itemId == item.id
    val haptic = LocalHapticFeedback.current
    val textRoom by rememberUpdatedState(LocalTextCardRoom.current)
    val baseViewConfiguration = LocalViewConfiguration.current
    val detailsLabel = stringResource(R.string.tier_detail_card_details)
    val dragViewConfiguration = remember(baseViewConfiguration) {
        ShortLongPressViewConfiguration(baseViewConfiguration, DRAG_LONG_PRESS_TIMEOUT_MILLIS)
    }

    DisposableEffect(item.id) {
        onDispose { dragController.unregisterTileBounds(item.id) }
    }

    // A lifted card keeps its row composed while autoscroll carries the row
    // off screen: the gesture lives in this tile and would end with it.
    val lifted = dragController.draggedPayload?.itemId == item.id
    val pinnable = LocalPinnableContainer.current
    DisposableEffect(lifted, pinnable) {
        val handle = if (lifted) pinnable?.pin() else null
        onDispose { handle?.release() }
    }

    CompositionLocalProvider(LocalViewConfiguration provides dragViewConfiguration) {
        Box(
            modifier = modifier
                .testTag(TierDetailTestTags.tile(item.id))
                .semantics(mergeDescendants = true) {
                    contentDescription = item.title
                    role = Role.Button
                    onClick(label = detailsLabel) {
                        onDoubleTap(item.id)
                        true
                    }
                }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key in DETAIL_KEYS) {
                        onDoubleTap(item.id)
                        true
                    } else {
                        false
                    }
                }
                .focusable()
                .onGloballyPositioned { coordinates ->
                    rootPosition = coordinates.positionInRoot()
                    sizePx = Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                    onPositioned(Rect(rootPosition, coordinates.size.toSize()))
                }
                .pointerInput(item.id, sourceTierId, scroll) {
                    detectTileDrag(
                        liftAxis = scroll.liftAxis(),
                        holdMillis = scroll.holdMillis(),
                        onDragStart = { offsetInTile ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragController.beginDrag(
                                DragPayload(
                                    itemId = item.id,
                                    title = item.title,
                                    imageUrl = item.imageUrl,
                                    sourceTierId = sourceTierId,
                                    width = width,
                                    height = height,
                                    textRoom = textRoom,
                                ),
                                rootPosition = rootPosition + offsetInTile,
                                homeCentre = rootPosition + sizePx / 2f,
                            )
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragController.updateDrag(dragAmount)
                        },
                        onDragEnd = {
                            when (val outcome = dragController.endDrag()) {
                                is DropOutcome.MoveTo -> onMoveItem(outcome.itemId, outcome.toTierId, outcome.toPosition)
                                is DropOutcome.Delete -> onDeleteItem(outcome.itemId)
                                null -> Unit
                            }
                        },
                        onDragCancel = { dragController.cancelDrag() },
                    )
                }
                .pointerInput(item.id) {
                    detectDoubleTapOnPress { onDoubleTap(item.id) }
                },
        ) {
            if (isDragging) {
                Box(if (collapseWhileLifted && dragController.returning == null) Modifier.size(0.dp) else Modifier.size(width, height))
            } else {
                ItemTile(item = item, width = width, height = height)
            }
        }
    }
}

@Composable
fun FloatingDragTile(dragController: TierDragController) {
    val media = TierYourLifeMedia.current
    val density = LocalDensity.current
    val borderAlpha = if (media.isDark) 0.14f else 0.6f
    val shape = RoundedCornerShape(6.dp)
    val readingDirection = LocalLayoutDirection.current

    // The overlay stands whether or not anything is being dragged. It learns
    // where it begins by being laid out, and that answer has to be in hand
    // before the first frame of a drag rather than one frame after it.
    // A card let go over nothing flies back to its place rather than vanishing.
    val trip = dragController.returning
    val back = remember(trip) { Animatable(0f) }
    LaunchedEffect(trip) {
        if (trip == null) return@LaunchedEffect
        back.animateTo(1f, tween(RETURN_MILLIS))
        dragController.finishReturn()
    }

    ForcedLeftToRightOverlay { origin ->
        val payload = dragController.draggedPayload ?: trip?.payload ?: return@ForcedLeftToRightOverlay
        val halfWidthPx = with(density) { (payload.width / 2).toPx() }
        val halfHeightPx = with(density) { (payload.height / 2).toPx() }

        Box(
            modifier = Modifier
                .absoluteOffset {
                    val position = if (dragController.draggedPayload == null && trip != null) {
                        lerp(trip.from, trip.to, back.value)
                    } else {
                        dragController.pointerPositionInRoot
                    }
                    // The pointer is measured against the window; this places
                    // against whatever pane the overlay sits in. Beside a
                    // column of boards those are four hundred points apart.
                    IntOffset(
                        x = (position.x - origin.x - halfWidthPx).roundToInt(),
                        y = (position.y - origin.y - halfHeightPx).roundToInt(),
                    )
                }
                .size(payload.width, payload.height)
                .testTag(TierDetailTestTags.FLOATING_DRAG_TILE)
                .graphicsLayer {
                    rotationZ = -4f
                    scaleX = 1.08f
                    scaleY = 1.08f
                }
                .shadow(elevation = 8.dp, shape = shape, clip = payload.imageUrl != null)
                .then(if (payload.imageUrl != null) Modifier.clip(shape).border(1.dp, Color.White.copy(alpha = borderAlpha), shape) else Modifier),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides readingDirection, LocalTextCardRoom provides payload.textRoom) {
                ItemTile(
                    item = TierItem(id = payload.itemId, title = payload.title, imageUrl = payload.imageUrl),
                    width = payload.width,
                    height = payload.height,
                )
            }
        }
    }
}

/** How long a card let go over nothing takes to settle back. */
private const val RETURN_MILLIS = 160
