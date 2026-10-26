package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.ROW_HOVER_TINT_ALPHA
import com.artiuillab.tieryourlife.feature.tier.board.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.board.common.rowTintFor
import com.artiuillab.tieryourlife.feature.tier.board.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.DRAG_LONG_PRESS_TIMEOUT_MILLIS
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.DraggableTile
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.ShortLongPressViewConfiguration
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDropOutcome
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode

/** Every band on every board: a caption lives inside this width, it never widens it. */
val TIER_BAND_WIDTH = 64.dp
val BAND_CAPTION_PADDING = 6.dp

const val CAPTION_HIDDEN_FONT_SCALE = 1.5f

@Composable
fun TierRow(
    tier: Tier,
    displayMode: TierListDisplayMode,
    dragController: TierDragController,
    rankedTierIds: List<Long>,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onReorderTiers: (List<Long>) -> Unit,
    onDeleteTier: (tierId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onDoubleTap: (itemId: Long) -> Unit = {},
    onEditTier: (tierId: Long) -> Unit = {},
    /** Whether the pool has anything to drop here; an empty row says where it would go only then. */
    poolHasCards: Boolean = false,
) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    val rowDensity = LocalTierRowDensity.current
    val isHovered = dragController.isDragging && dragController.hoveredTierId == tier.id
    val surface = MaterialTheme.colorScheme.surface
    val rowBackground = if (isHovered) rowTintFor(colors.band, surface, ROW_HOVER_TINT_ALPHA) else colors.rowTint

    DisposableEffect(tier.id) {
        onDispose { dragController.unregisterRowBounds(tier.id) }
    }

    val collapsed = dragController.isDraggingTier || dragController.isSettlingTier
    val strip = displayMode == TierListDisplayMode.HORIZONTAL_SCROLL
    val stripState = rememberLazyListState()

    if (strip) {
        SideEffect {
            dragController.registerItemsRowMeta(tier.id, stripState, tier.items.map { it.id })
        }
    }

    DisposableEffect(tier.id, strip) {
        onDispose { if (strip) dragController.unregisterItemsRow(tier.id) }
    }

    val rowHeightModifier = if (collapsed || displayMode == TierListDisplayMode.HORIZONTAL_SCROLL) {
        Modifier.height(rowDensity.occupiedHeight)
    } else {
        Modifier.height(IntrinsicSize.Min)
    }

    var bandRootPosition by remember { mutableStateOf(Offset.Zero) }
    var bandWidthPx by remember { mutableFloatStateOf(0f) }
    val baseViewConfiguration = LocalViewConfiguration.current
    val dragViewConfiguration = remember(baseViewConfiguration) {
        ShortLongPressViewConfiguration(baseViewConfiguration, DRAG_LONG_PRESS_TIMEOUT_MILLIS)
    }
    val density = LocalDensity.current
    val slotHeightPx = with(density) { (rowDensity.occupiedHeight + rowDensity.rowGap).toPx() }

    Box(modifier.fillMaxWidth()) {
        val bandWidth = rowDensity.bandWidth
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(rowHeightModifier)
                .animateContentSize(animationSpec = tween(durationMillis = 200))
                .clip(RoundedCornerShape(12.dp))
                .onGloballyPositioned { coordinates -> dragController.registerRowBounds(tier.id, coordinates.boundsInRoot()) }
                .testTag(TierDetailTestTags.tierRow(tier.id))
                .background(rowBackground)
                .then(
                    if (isHovered) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    },
                ),
        ) {
        CompositionLocalProvider(LocalViewConfiguration provides dragViewConfiguration) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(bandWidth)
                    .background(colors.band)
                    .testTag(TierDetailTestTags.tierBand(tier.id))
                    .then(
                        if (!tier.isPool) {
                            Modifier
                                .onGloballyPositioned { coordinates ->
                                    bandRootPosition = coordinates.positionInRoot()
                                    bandWidthPx = coordinates.size.width.toFloat()
                                }
                                .pointerInput(tier.id, rankedTierIds) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offsetInBand ->
                                            dragController.beginTierDrag(
                                                tierId = tier.id,
                                                rankedTierIds = rankedTierIds,
                                                rootPosition = bandRootPosition + offsetInBand,
                                                bandWidthPx = bandWidthPx,
                                                slotHeightPx = slotHeightPx,
                                            )
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragController.updateTierDrag(dragAmount)
                                        },
                                        onDragEnd = {
                                            when (val outcome = dragController.endTierDrag()) {
                                                is TierDropOutcome.Reorder -> onReorderTiers(outcome.orderedTierIds)
                                                is TierDropOutcome.Delete -> onDeleteTier(outcome.tierId)
                                                null -> Unit
                                            }
                                        },
                                        onDragCancel = { dragController.cancelTierDrag() },
                                    )
                                }
                                .pointerInput(tier.id) {
                                    detectTapGestures(onDoubleTap = { onEditTier(tier.id) })
                                }
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = BAND_CAPTION_PADDING, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = tier.label,
                    style = TierYourLifeType.current.tierBandLetter.merge(
                        TextStyle(fontSize = rowDensity.letterSize, lineHeight = rowDensity.letterLineHeight),
                    ),
                    color = colors.onBand,
                )
                val captionShown = rowDensity.captionOnEmptyRows || tier.items.isNotEmpty()
                tier.caption?.takeIf { captionShown }?.let { caption ->
                    if (LocalDensity.current.fontScale < CAPTION_HIDDEN_FONT_SCALE) {
                        Text(
                            text = caption,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = TierYourLifeType.current.tierBandCaption.merge(
                                TextStyle(fontSize = rowDensity.captionSize, lineHeight = rowDensity.captionLineHeight),
                            ),
                            color = colors.onBand.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        if (collapsed) {
            CollapsedItemCount(
                count = tier.items.size,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(TierDetailTestTags.tierItems(tier.id)),
            )
        } else if (displayMode == TierListDisplayMode.HORIZONTAL_SCROLL) {
            // Lazy, like the pool: the drag controller works out the drop from
            // the list's own layout.
            LazyRow(
                state = stripState,
                modifier = Modifier
                    .weight(1f)
                    .height(rowDensity.occupiedHeight)
                    .onGloballyPositioned { coordinates ->
                        dragController.registerItemsRowBounds(tier.id, coordinates.boundsInRoot())
                    }
                    .testTag(TierDetailTestTags.tierItems(tier.id)),
                contentPadding = PaddingValues(rowDensity.padding),
                horizontalArrangement = Arrangement.spacedBy(rowDensity.gap),
            ) {
                items(tier.items, key = { it.id }) { item ->
                    DraggableTile(
                        item = item,
                        sourceTierId = tier.id,
                        width = rowDensity.tileWidth,
                        height = rowDensity.tileHeight,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = onDeleteItem,
                        onDoubleTap = onDoubleTap,
                    )
                }
            }
        } else {
            // Where the lifted card would land, drawn while it hovers: the
            // row's own cards make room for it, and its old place takes none.
            val slot = if (isHovered) dragController.hoverIndex else null
            val liftedId = dragController.draggedPayload?.itemId
            val showHint = tier.items.isEmpty() && slot == null && rowDensity.emptyHint && poolHasCards
            FlowRow(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = rowDensity.emptyHeight)
                    .padding(rowDensity.padding)
                    .testTag(TierDetailTestTags.tierItems(tier.id)),
                horizontalArrangement = Arrangement.spacedBy(rowDensity.gap),
                verticalArrangement = Arrangement.spacedBy(rowDensity.gap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (showHint) {
                    Text(
                        text = stringResource(R.string.tier_detail_drop_items_here),
                        modifier = Modifier
                            .heightIn(min = rowDensity.emptyHeight - rowDensity.padding * 2)
                            .padding(start = 9.dp)
                            .wrapContentHeight(Alignment.CenterVertically),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                var placed = 0
                tier.items.forEachIndexed { index, item ->
                    if (item.id != liftedId && slot == placed) DropSlot(rowDensity)
                    DraggableTile(
                        item = item,
                        sourceTierId = tier.id,
                        width = rowDensity.tileWidth,
                        height = rowDensity.tileHeight,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = onDeleteItem,
                        onPositioned = { bounds -> dragController.registerTileBounds(tier.id, item.id, index, bounds) },
                        onDoubleTap = onDoubleTap,
                        collapseWhileLifted = slot != null,
                    )
                    if (item.id != liftedId) placed++
                }
                if (slot != null && slot >= placed) DropSlot(rowDensity)
            }
        }
        }
    }
}

@Composable
private fun CollapsedItemCount(count: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
        Text(
            text = pluralStringResource(R.plurals.tier_detail_collapsed_item_count, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TierRowLightPreview() = TierYourLifeTheme(false) {
    TierRow(
        tier = previewTierList.tiers.first(),
        displayMode = TierListDisplayMode.WRAP,
        dragController = remember { TierDragController() },
        rankedTierIds = previewTierList.tiers.filterNot { it.isPool }.map { it.id },
        onMoveItem = { _, _, _ -> },
        onDeleteItem = {},
        onReorderTiers = {},
        onDeleteTier = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun TierRowDarkPreview() = TierYourLifeTheme(true) {
    TierRow(
        tier = previewTierList.tiers.first(),
        displayMode = TierListDisplayMode.WRAP,
        dragController = remember { TierDragController() },
        rankedTierIds = previewTierList.tiers.filterNot { it.isPool }.map { it.id },
        onMoveItem = { _, _, _ -> },
        onDeleteItem = {},
        onReorderTiers = {},
        onDeleteTier = {},
    )
}

/** The place a lifted card would land: its own size, dashed. */
@Composable
private fun DropSlot(rowDensity: TierRowDensity) {
    Box(
        Modifier
            .size(rowDensity.tileWidth, rowDensity.tileHeight)
            .dashedBorder(width = 2.dp, color = MaterialTheme.colorScheme.primary, cornerRadius = 6.dp)
            .testTag(TierDetailTestTags.DROP_SLOT),
    )
}
