package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.ui.AutoAwesomeIcon
import com.artiuillab.tieryourlife.core.theme.ui.CheckCircleIcon
import com.artiuillab.tieryourlife.core.theme.ui.PanelCloseIcon
import com.artiuillab.tieryourlife.core.theme.ui.PanelOpenIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.DraggableTile
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier

/** Open in a short window: three cards abreast, readable and draggable. Nothing narrower is either. */
val POOL_COLUMN_WIDTH: Dp = 216.dp

/** Open beside a board held sideways on a tablet; a little narrower when the pane of boards stands beside it too. */
val POOL_COLUMN_WIDE_WIDTH: Dp = 300.dp
val POOL_COLUMN_WIDE_WIDTH_BESIDE_INDEX: Dp = 268.dp

/** Folded: a strip with the way back, the count and Add. */
val POOL_STRIP_WIDTH: Dp = 56.dp

/** Around the column, so it reads as a card beside the rows rather than a wall. */
val POOL_COLUMN_MARGIN: Dp = 8.dp

private val POOL_TILE_WIDTH = 52.dp
private val POOL_TILE_HEIGHT = 76.dp
private val POOL_CORNER = 16.dp
private const val WIDTH_MILLIS = 200

/** Everything placed: the success green of the design, light and dark. */
private val PlacedLight = Color(0xFF3F7F55)
private val PlacedDark = Color(0xFF7BD394)

/**
 * The pool beside the rows instead of under them, for a window whose height
 * is better spent on rows: a phone held sideways, a tablet held as one. Open
 * while anything is left to rank, a card as tall as what it holds; folded to
 * a strip when it is empty or the person folds it. The person's choice is kept
 * on this device and wins until the pool's count changes. Cards are dragged
 * out of it and back exactly as from the strip under the rows; a drop lands
 * where the finger points, by the rule the rows use for their grids. While a
 * card is lifted the head becomes the trash.
 */
@Composable
fun PoolColumn(
    pool: Tier,
    onAddClick: () -> Unit,
    onGenerateClick: () -> Unit,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onDoubleTap: (itemId: Long) -> Unit = {},
    readOnly: Boolean = false,
    /** A tablet's column: wider, four cards abreast, a larger head. */
    roomy: Boolean = false,
    indexBeside: Boolean = false,
    /** A line under the pool's title from the screen around the board: a reader's word on what their copy is. */
    note: (@Composable () -> Unit)? = null,
    /**
     * Which of a screen's arrangements is shown: the choice then belongs to
     * that side alone, kept for this screen rather than for the device, and
     * starts again from the rule each time the side changes.
     */
    side: Any? = null,
) {
    val memory = LocalBoardLayoutMemory.current
    val count = pool.items.size
    var sideChoice by rememberSaveable(pool.id, side) { mutableStateOf<Boolean?>(null) }
    // The count the person's choice was made against; a different count hands
    // the decision back to the board.
    var seenCount by rememberSaveable(pool.id) { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count != seenCount) {
            if (side == null) memory.poolColumnCollapsed = null
            sideChoice = null
            seenCount = count
        }
    }
    val choice = if (side != null) sideChoice else memory.poolColumnCollapsed
    val collapsed = choice ?: (count == 0)
    val choose: (Boolean) -> Unit = { fold -> if (side != null) sideChoice = fold else memory.poolColumnCollapsed = fold }
    val openWidth = openColumnWidth(roomy, indexBeside)
    val width by animateDpAsState(if (collapsed) POOL_STRIP_WIDTH else openWidth, tween(WIDTH_MILLIS), label = "pool column width")
    val isHovered = dragController.isDragging && dragController.hoveredTierId == pool.id
    val lifting = dragController.draggedPayload != null && !readOnly

    // The strip under the rows registered its row of cards for drops along
    // it; a column has none, so a drop here goes by the cards' own places.
    SideEffect { dragController.unregisterItemsRow(pool.id) }
    DisposableEffect(pool.id) {
        onDispose { dragController.unregisterRowBounds(pool.id) }
    }

    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(POOL_CORNER))
            .onGloballyPositioned { coordinates -> dragController.registerRowBounds(pool.id, coordinates.boundsInRoot()) }
            .testTag(TierDetailTestTags.POOL_COLUMN)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (isHovered) {
                    Modifier.dashedBorder(width = 2.dp, color = MaterialTheme.colorScheme.primary, cornerRadius = POOL_CORNER)
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (collapsed) {
            FoldedStrip(
                count = count,
                readOnly = readOnly,
                lifting = lifting,
                dragController = dragController,
                onOpen = { choose(false) },
                onAddClick = onAddClick,
            )
            return@Column
        }
        OpenColumnHead(
            count = count,
            roomy = roomy,
            readOnly = readOnly,
            lifting = lifting,
            dragController = dragController,
            note = note,
            onFold = { choose(true) },
            onAddClick = onAddClick,
            onGenerateClick = onGenerateClick,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (roomy) 4 else 3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .testTag(TierDetailTestTags.POOL_ITEMS),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (roomy) 10.dp else 8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(if (roomy) 10.dp else 8.dp),
        ) {
            itemsIndexed(pool.items, key = { _, item -> item.id }) { index, item ->
                Box(contentAlignment = Alignment.Center) {
                    DraggableTile(
                        item = item,
                        sourceTierId = pool.id,
                        width = POOL_TILE_WIDTH,
                        height = POOL_TILE_HEIGHT,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = onDeleteItem,
                        onPositioned = { bounds -> dragController.registerTileBounds(pool.id, item.id, index, bounds) },
                        onDoubleTap = onDoubleTap,
                    )
                }
            }
        }
    }
}

/** The open column's head: the count and the way to fold it, the reader's line, the add field; or, while a card is lifted, the trash. */
private fun openColumnWidth(roomy: Boolean, indexBeside: Boolean): Dp = when {
    !roomy -> POOL_COLUMN_WIDTH
    indexBeside -> POOL_COLUMN_WIDE_WIDTH_BESIDE_INDEX
    else -> POOL_COLUMN_WIDE_WIDTH
}

@Composable
private fun OpenColumnHead(
    count: Int,
    roomy: Boolean,
    readOnly: Boolean,
    lifting: Boolean,
    dragController: TierDragController,
    note: (@Composable () -> Unit)?,
    onFold: () -> Unit,
    onAddClick: () -> Unit,
    onGenerateClick: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        if (lifting) {
            PoolHeadTrash(dragController = dragController, caption = stringResource(R.string.tier_detail_drop_unrank_column))
            return@Column
        }
        OpenHead(count = count, roomy = roomy, onFold = onFold)
        note?.let { line -> Box(Modifier.fillMaxWidth().padding(bottom = 6.dp)) { line() } }
        if (!readOnly) {
            AddCardField(onAddClick = onAddClick, onGenerateClick = onGenerateClick, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun OpenHead(count: Int, roomy: Boolean, onFold: () -> Unit) {
    val foldDescription = pluralStringResource(R.plurals.tier_detail_ranked_pool_expanded_description, count, count)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.tier_detail_pool_count, count),
            modifier = Modifier.weight(1f),
            style = if (roomy) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = onFold,
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = foldDescription }
                .testTag(TierDetailTestTags.POOL_TOGGLE),
        ) { PanelCloseIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/**
 * Where a card is added, at the head of the pool it goes into: a field that
 * opens the add flow, and the AI studio at its end where it is offered.
 */
@Composable
internal fun AddCardField(onAddClick: () -> Unit, onGenerateClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onAddClick)
            .testTag(TierDetailTestTags.ADD_CHIP)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.tier_detail_add_card),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (Features.GENERATION_OFFERED) {
            val description = stringResource(R.string.cd_ai_chip)
            IconButton(
                onClick = onGenerateClick,
                modifier = Modifier
                    .size(36.dp)
                    .semantics { contentDescription = description }
                    .testTag(TierDetailTestTags.GENERATE_CHIP),
            ) { AutoAwesomeIcon(18.dp, MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun FoldedStrip(
    count: Int,
    readOnly: Boolean,
    lifting: Boolean,
    dragController: TierDragController,
    onOpen: () -> Unit,
    onAddClick: () -> Unit,
) {
    val openDescription = pluralStringResource(R.plurals.tier_detail_ranked_pool_collapsed_description, count, count)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (lifting) {
            PoolStripTrash(dragController = dragController)
        }
        IconButton(
            onClick = onOpen,
            modifier = Modifier
                .semantics { contentDescription = openDescription }
                .testTag(TierDetailTestTags.POOL_TOGGLE),
        ) { PanelOpenIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
        if (count == 0) {
            val placed = if (TierYourLifeMedia.current.isDark) PlacedDark else PlacedLight
            CheckCircleIcon(22.dp, placed, MaterialTheme.colorScheme.surfaceContainer)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!readOnly) {
            val addDescription = stringResource(R.string.tier_detail_add_card)
            FilledTonalIconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = addDescription },
            ) { PlusIcon(20.dp, MaterialTheme.colorScheme.onSecondaryContainer) }
        }
    }
}

@Preview(widthDp = 320, heightDp = 400, showBackground = true)
@Composable
private fun PoolColumnPreview() = TierYourLifeTheme(false) {
    PoolColumn(
        pool = previewTierList.tiers.first { it.isPool },
        onAddClick = {},
        onGenerateClick = {},
        dragController = remember { TierDragController() },
        onMoveItem = { _, _, _ -> },
        onDeleteItem = {},
    )
}
