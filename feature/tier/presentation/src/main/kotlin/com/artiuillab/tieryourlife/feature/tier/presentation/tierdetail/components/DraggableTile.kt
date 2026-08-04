package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import kotlin.math.roundToInt

// Shared by every tile in the pool and every tile in a ranked tier so the two
// physically cannot drift apart: same gesture, same lift/hover/drop behaviour.
// Only the size differs, passed in by the caller.
@Composable
internal fun DraggableTile(
    item: TierItem,
    sourceTierId: Long,
    width: Dp,
    height: Dp,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    val isDragging = dragController.draggedPayload?.itemId == item.id

    Box(
        modifier = modifier
            .testTag(TierDetailTestTags.tile(item.id))
            .onGloballyPositioned { coordinates -> rootPosition = coordinates.positionInRoot() }
            .pointerInput(item.id, sourceTierId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offsetInTile ->
                        dragController.beginDrag(
                            DragPayload(
                                itemId = item.id,
                                title = item.title,
                                imageUrl = item.imageUrl,
                                sourceTierId = sourceTierId,
                                width = width,
                                height = height,
                            ),
                            rootPosition = rootPosition + offsetInTile,
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragController.updateDrag(dragAmount)
                    },
                    onDragEnd = {
                        dragController.endDrag()?.let { drop ->
                            onMoveItem(drop.itemId, drop.toTierId, drop.toPosition)
                        }
                    },
                    onDragCancel = { dragController.cancelDrag() },
                )
            },
    ) {
        if (isDragging) {
            // The tile stays composed (so the gesture above keeps running) but is
            // drawn empty while lifted: its floating copy is what the user sees,
            // painted separately at the screen root so it isn't clipped by this row.
            Box(Modifier.size(width, height))
        } else {
            ItemTile(item = item, width = width, height = height)
        }
    }
}

@Composable
internal fun FloatingDragTile(dragController: TierDragController) {
    val payload = dragController.draggedPayload ?: return
    val position = dragController.pointerPositionInRoot
    val media = TierYourLifeMedia.current
    val density = LocalDensity.current
    val halfWidthPx = with(density) { (payload.width / 2).toPx() }
    val halfHeightPx = with(density) { (payload.height / 2).toPx() }
    val borderAlpha = if (media.isDark) 0.14f else 0.6f
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (position.x - halfWidthPx).roundToInt(),
                    y = (position.y - halfHeightPx).roundToInt(),
                )
            }
            .size(payload.width, payload.height)
            .graphicsLayer {
                rotationZ = -4f
                scaleX = 1.06f
                scaleY = 1.06f
            }
            .shadow(elevation = 12.dp, shape = shape)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = borderAlpha), shape),
    ) {
        ItemTile(
            item = TierItem(id = payload.itemId, title = payload.title, imageUrl = payload.imageUrl),
            width = payload.width,
            height = payload.height,
        )
    }
}
