package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ROW_HOVER_TINT_ALPHA
import com.artiuillab.tieryourlife.feature.tier.presentation.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.presentation.common.rowTintFor
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags

// FlowRow (unlike the old LazyRow) supports intrinsic measurement, so
// IntrinsicSize.Min below can stretch the band to match its wrapped height.
private val MIN_TIER_ROW_HEIGHT = 84.dp

@Composable
internal fun TierRow(
    tier: Tier,
    displayMode: TierListDisplayMode,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onDoubleTap: (itemId: Long) -> Unit = {},
) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    val isHovered = dragController.isDragging && dragController.hoveredTierId == tier.id
    val surface = MaterialTheme.colorScheme.surface
    val rowBackground = if (isHovered) rowTintFor(colors.band, surface, ROW_HOVER_TINT_ALPHA) else colors.rowTint

    // A horizontally-scrolling child has unbounded intrinsic width, which IntrinsicSize.Min
    // can't resolve cleanly — its height is already fixed, so it doesn't need an intrinsic
    // pass at all; only the wrapping FlowRow (whose height depends on how much it wraps)
    // still needs one to let the label column stretch to match it.
    val rowHeightModifier = if (displayMode == TierListDisplayMode.HORIZONTAL_SCROLL) {
        Modifier.height(MIN_TIER_ROW_HEIGHT)
    } else {
        Modifier.height(IntrinsicSize.Min)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(rowHeightModifier)
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
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(66.dp)
                .background(colors.band)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = tier.label,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onBand,
            )
            tier.caption?.let { caption ->
                Text(
                    text = caption,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = colors.onBand.copy(alpha = 0.7f),
                )
            }
        }

        // HORIZONTAL_SCROLL is the only mode that changes this row: everything else about
        // it (label column, tile size, spacing, padding, drag/double-tap wiring) is the
        // same regardless of mode, only the items container differs — a single
        // non-wrapping, horizontally scrollable line instead of one that wraps and grows
        // taller. FLAT_RANKED isn't drawn yet, so it falls back to the wrap behaviour too.
        if (displayMode == TierListDisplayMode.HORIZONTAL_SCROLL) {
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

@Composable
internal fun ItemTile(item: TierItem, width: Dp, height: Dp) {
    val media = TierYourLifeMedia.current
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(media.tilePlaceholder),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (item.imageUrl != null) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = item.title.take(6).uppercase(),
                modifier = Modifier.padding(bottom = 4.dp),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = media.tileLabel,
            )
        }
    }
}
