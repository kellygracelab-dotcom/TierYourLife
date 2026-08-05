package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ROW_HOVER_TINT_ALPHA
import com.artiuillab.tieryourlife.feature.tier.presentation.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.presentation.common.rowTintFor
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// FlowRow (unlike the old LazyRow) supports intrinsic measurement, so
// IntrinsicSize.Min below can stretch the band to match its wrapped height.
// Also the collapsed row height: "the same as a one-line row of posters now" (see the
// task this shipped with) is exactly this value, not a second constant.
private val MIN_TIER_ROW_HEIGHT = 84.dp

private val MIN_TIER_BAND_WIDTH = 56.dp
private const val MAX_TIER_BAND_FRACTION = 1f / 3f

// docs/design-spec-turns-8-9.md, section 3: "The caption disappears entirely at a
// system font scale of >=1.5x. The large tier letter always stays."
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
    onDoubleTap: (itemId: Long) -> Unit = {},
    onEditTier: (tierId: Long) -> Unit = {},
) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    val isHovered = dragController.isDragging && dragController.hoveredTierId == tier.id
    val surface = MaterialTheme.colorScheme.surface
    val rowBackground = if (isHovered) rowTintFor(colors.band, surface, ROW_HOVER_TINT_ALPHA) else colors.rowTint

    // A deleted (or reordered-away) tier's registered bounds would otherwise sit in
    // TierDragController's map forever, marking a rectangle for a row that no longer
    // exists — this is what removes it the moment this composable actually leaves.
    DisposableEffect(tier.id) {
        onDispose { dragController.unregisterRowBounds(tier.id) }
    }

    // Collapsing is triggered by any tier being lifted, not just this one — every ranked
    // row (including the one under the finger) collapses to the same uniform height, per
    // the task: with everything one height, the drop index is a plain position compare,
    // no auto-scroll needed.
    val collapsed = dragController.isDraggingTier

    // A horizontally-scrolling child has unbounded intrinsic width, which IntrinsicSize.Min
    // can't resolve cleanly — its height is already fixed, so it doesn't need an intrinsic
    // pass at all; collapsed content (a line of text) is fixed-height for the same reason.
    val rowHeightModifier = if (collapsed || displayMode == TierListDisplayMode.HORIZONTAL_SCROLL) {
        Modifier.height(MIN_TIER_ROW_HEIGHT)
    } else {
        Modifier.height(IntrinsicSize.Min)
    }

    var bandRootPosition by remember { mutableStateOf(Offset.Zero) }
    var bandWidthPx by remember { mutableStateOf(0f) }
    val baseViewConfiguration = LocalViewConfiguration.current
    val dragViewConfiguration = remember(baseViewConfiguration) {
        ShortLongPressViewConfiguration(baseViewConfiguration, DRAG_LONG_PRESS_TIMEOUT_MILLIS)
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
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
                                // Independent pointerInput from the double-tap one below, same
                                // reasoning DraggableTile's own two recognizers rely on: a quick
                                // tap never reaches onDragStart (long-press-gated), so it can't
                                // steal a real double-tap, and a genuine long-press's consumed
                                // move events fall outside detectTapGestures' up-without-movement
                                // check — the two can't both fire for the same gesture.
                                .pointerInput(tier.id, rankedTierIds) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offsetInBand ->
                                            dragController.beginTierDrag(
                                                tierId = tier.id,
                                                rankedTierIds = rankedTierIds,
                                                rootPosition = bandRootPosition + offsetInBand,
                                                bandWidthPx = bandWidthPx,
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
                                // A separate pointerInput from both the drag above and the tiles
                                // below, so this gesture can only ever claim touches that land on
                                // the band itself — the tiles keep their own drag/double-tap
                                // recognizers exactly as before, untouched by this one.
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

@Composable
internal fun ItemTile(item: TierItem, width: Dp, height: Dp) {
    val media = TierYourLifeMedia.current
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(media.tilePlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        if (item.imageUrl != null) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // docs/design-spec-turns-8-9.md, section 10.4: "Missing artwork falls back to
            // the title, centred, ellipsised, up to 2 lines" — the real fallback the
            // design settled on, not a 6-letter uppercase abbreviation. Type role is
            // section 2's "caption on a poster placeholder" (bodySmall, 12sp/16sp).
            Text(
                text = item.title,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = media.tileLabel,
            )
        }
    }
}

@Composable
internal fun FloatingDragRow(dragController: TierDragController, tier: Tier?) {
    if (tier == null) return
    val position = dragController.tierPointerPositionInRoot
    val density = LocalDensity.current
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    // Half of the dragged tier's own band width, captured live at drag start (TierRow's
    // onGloballyPositioned -> beginTierDrag) rather than half of a fixed column width —
    // the band is wrap-content between MIN_TIER_BAND_WIDTH and MAX_TIER_BAND_FRACTION of
    // the row (see those constants above), so its width depends on this tier's own
    // caption length and can no longer be assumed to be a constant 66dp.
    val halfWidthPx = dragController.draggedTierBandWidthPx / 2f
    val halfHeightPx = with(density) { (MIN_TIER_ROW_HEIGHT / 2).toPx() }
    val shape = RoundedCornerShape(12.dp)

    val readingDirection = LocalLayoutDirection.current
    val rightToLeft = readingDirection == LayoutDirection.Rtl

    // Placed from a root coordinate, so its own placement must not follow the reading
    // direction — see ForcedLeftToRightOverlay. The row's contents are drawn back in the real
    // direction inside, so the band still sits on the correct side.
    ForcedLeftToRightOverlay {
      CompositionLocalProvider(LocalLayoutDirection provides readingDirection) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val bandMaxWidth = maxWidth * MAX_TIER_BAND_FRACTION
        val rowWidthPx = with(density) { maxWidth.toPx() }
        Row(
            modifier = Modifier
                .absoluteOffset {
                    // The finger is holding the coloured band, and the row is dragged by it,
                    // so the offset has to put the band under the pointer — not the row's
                    // left edge. The band is drawn at the row's leading edge, which is the
                    // right-hand end in Arabic, so a full-width row placed from its left edge
                    // there would hang the band off the screen entirely.
                    val left = if (rightToLeft) {
                        position.x + halfWidthPx - rowWidthPx
                    } else {
                        position.x - halfWidthPx
                    }
                    IntOffset(
                        x = left.roundToInt(),
                        y = (position.y - halfHeightPx).roundToInt(),
                    )
                }
                .fillMaxWidth()
                .height(MIN_TIER_ROW_HEIGHT)
                .graphicsLayer {
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
                .shadow(elevation = 8.dp, shape = shape)
                .clip(shape)
                .background(colors.rowTint)
                .border(1.dp, Color.White.copy(alpha = if (TierYourLifeMedia.current.isDark) 0.14f else 0.6f), shape),
        ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = MIN_TIER_BAND_WIDTH, max = bandMaxWidth)
                .background(colors.band)
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
        CollapsedItemCount(count = tier.items.size, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
      }
    }
}
