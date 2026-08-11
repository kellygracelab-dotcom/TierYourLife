package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ROW_HOVER_TINT_ALPHA
import com.artiuillab.tieryourlife.feature.tier.presentation.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.presentation.common.rowTintFor
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.DRAG_LONG_PRESS_TIMEOUT_MILLIS
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.DraggableTile
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.TIER_LIST_ITEM_SPACING
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.ShortLongPressViewConfiguration
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.TierDropOutcome

private val MIN_TIER_ROW_HEIGHT = 84.dp

private val MIN_TIER_BAND_WIDTH = 56.dp
private const val MAX_TIER_BAND_FRACTION = 1f / 3f

internal const val CAPTION_HIDDEN_FONT_SCALE = 1.5f

@Composable
internal fun TierRow(
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
) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    val isHovered = dragController.isDragging && dragController.hoveredTierId == tier.id
    val surface = MaterialTheme.colorScheme.surface
    val rowBackground = if (isHovered) rowTintFor(colors.band, surface, ROW_HOVER_TINT_ALPHA) else colors.rowTint

    DisposableEffect(tier.id) {
        onDispose { dragController.unregisterRowBounds(tier.id) }
    }

    val collapsed = dragController.isDraggingTier || dragController.isSettlingTier

    val rowHeightModifier = if (collapsed || displayMode == TierListDisplayMode.HORIZONTAL_SCROLL) {
        Modifier.height(MIN_TIER_ROW_HEIGHT)
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
    val slotHeightPx = with(density) { (MIN_TIER_ROW_HEIGHT + TIER_LIST_ITEM_SPACING).toPx() }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val bandMaxWidth = maxWidth * MAX_TIER_BAND_FRACTION
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
                        Modifier.dashedBorder(width = 2.dp, color = colors.band, cornerRadius = 12.dp)
                    } else {
                        Modifier
                    },
                ),
        ) {
        CompositionLocalProvider(LocalViewConfiguration provides dragViewConfiguration) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = MIN_TIER_BAND_WIDTH, max = bandMaxWidth)
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
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = tier.label,
                    style = TierYourLifeType.current.tierBandLetter,
                    color = colors.onBand,
                )
                tier.caption?.let { caption ->
                    if (LocalDensity.current.fontScale < CAPTION_HIDDEN_FONT_SCALE) {
                        Text(
                            text = caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TierYourLifeType.current.tierBandCaption,
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(MIN_TIER_ROW_HEIGHT)
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
                    .testTag(TierDetailTestTags.tierItems(tier.id)),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tier.items.forEachIndexed { index, item ->
                    DraggableTile(
                        item = item,
                        sourceTierId = tier.id,
                        width = 44.dp,
                        height = 64.dp,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = onDeleteItem,
                        onPositioned = { bounds -> dragController.registerTileBounds(tier.id, item.id, index, bounds) },
                        onDoubleTap = onDoubleTap,
                    )
                }
            }
        } else {
            FlowRow(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MIN_TIER_ROW_HEIGHT)
                    .padding(10.dp)
                    .testTag(TierDetailTestTags.tierItems(tier.id)),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tier.items.forEachIndexed { index, item ->
                    DraggableTile(
                        item = item,
                        sourceTierId = tier.id,
                        width = 44.dp,
                        height = 64.dp,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = onDeleteItem,
                        onPositioned = { bounds -> dragController.registerTileBounds(tier.id, item.id, index, bounds) },
                        onDoubleTap = onDoubleTap,
                    )
                }
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
