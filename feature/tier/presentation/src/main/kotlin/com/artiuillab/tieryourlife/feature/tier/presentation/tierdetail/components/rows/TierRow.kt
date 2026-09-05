package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ROW_HOVER_TINT_ALPHA
import com.artiuillab.tieryourlife.feature.tier.presentation.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.presentation.common.rowTintFor
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.DRAG_LONG_PRESS_TIMEOUT_MILLIS
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.DraggableTile
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.ShortLongPressViewConfiguration
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.TIER_LIST_ITEM_SPACING
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag.TierDropOutcome
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.previewTierList

private val MIN_TIER_ROW_HEIGHT = 84.dp

private val MIN_TIER_BAND_WIDTH = 56.dp

/**
 * A third of the row is the right ceiling on a phone and the wrong one on a
 * 1080dp board, where a third is 360dp. The tighter of the fraction and this
 * number wins.
 */
private val MAX_TIER_BAND_WIDTH = 128.dp
private const val MAX_TIER_BAND_FRACTION = 1f / 3f
internal val BAND_CAPTION_PADDING = 6.dp

internal const val CAPTION_HIDDEN_FONT_SCALE = 1.5f

/**
 * The captions the band width is measured from, leaving out the tier being
 * edited: its caption comes from the field rather than from the board.
 */
internal fun TierList.captionsExcept(tierId: Long?): List<String> = tiers
    .filterNot { it.isPool || it.id == tierId }
    .mapNotNull { it.caption?.takeIf(String::isNotBlank) }

/**
 * One width for every band on a board, from the longest caption: the letters
 * down the left are the spine of a tier list and only work as a column. The
 * answer differs between languages; what has to agree is one board.
 */
@Composable
internal fun rememberBandContentWidth(tiers: List<Tier>): Dp =
    rememberBandContentWidth(tiers.filterNot { it.isPool }.mapNotNull { it.caption?.takeIf(String::isNotBlank) })

@Composable
internal fun rememberBandContentWidth(
    captions: List<String>,
    measurer: TextMeasurer = rememberTextMeasurer(),
    style: TextStyle = TierYourLifeType.current.tierBandCaption,
): Dp {
    val density = LocalDensity.current
    // Density and fontScale are read, not passed: both can change while the
    // board is open.
    return remember(captions, style, density.density, density.fontScale) {
        val widest = captions.maxOfOrNull { measurer.measure(it, style, maxLines = 1).size.width } ?: 0
        with(density) { widest.toDp() } + BAND_CAPTION_PADDING * 2
    }
}

@Composable
internal fun TierRow(
    tier: Tier,
    bandContentWidth: Dp,
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
        // A caption longer than a third of the row is the one that wraps; the
        // rest take the same width rather than following it out.
        val bandWidth = minOf(
            maxOf(MIN_TIER_BAND_WIDTH, bandContentWidth),
            maxWidth * MAX_TIER_BAND_FRACTION,
            MAX_TIER_BAND_WIDTH,
        )
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
                    .padding(start = BAND_CAPTION_PADDING, end = BAND_CAPTION_PADDING, top = 10.dp),
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
            // Lazy, like the pool: the drag controller works out the drop from
            // the list's own layout.
            LazyRow(
                state = stripState,
                modifier = Modifier
                    .weight(1f)
                    .height(MIN_TIER_ROW_HEIGHT)
                    .onGloballyPositioned { coordinates ->
                        dragController.registerItemsRowBounds(tier.id, coordinates.boundsInRoot())
                    }
                    .testTag(TierDetailTestTags.tierItems(tier.id)),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tier.items, key = { it.id }) { item ->
                    DraggableTile(
                        item = item,
                        sourceTierId = tier.id,
                        width = 44.dp,
                        height = 64.dp,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = onDeleteItem,
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
        bandContentWidth = 64.dp,
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
        bandContentWidth = 64.dp,
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
