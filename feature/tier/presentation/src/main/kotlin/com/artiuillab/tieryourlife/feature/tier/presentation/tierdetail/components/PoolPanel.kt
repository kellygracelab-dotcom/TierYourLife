package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags

@Composable
internal fun PoolPanel(
    pool: Tier,
    onAddClick: () -> Unit,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onDoubleTap: (itemId: Long) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val isHovered = dragController.isDragging && dragController.hoveredTierId == pool.id

    SideEffect {
        dragController.registerItemsRowMeta(pool.id, listState, pool.items.map { it.id })
    }

    // The pool doesn't actually leave composition in practice (exactly one always
    // exists), but it registers bounds the same way every other target does, so it
    // unregisters the same way too rather than being a silent exception to the rule.
    DisposableEffect(pool.id) {
        onDispose {
            dragController.unregisterRowBounds(pool.id)
            dragController.unregisterItemsRow(pool.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .onGloballyPositioned { coordinates -> dragController.registerRowBounds(pool.id, coordinates.boundsInRoot()) }
            .testTag(TierDetailTestTags.POOL_PANEL)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (isHovered) {
                    Modifier.dashedBorder(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        cornerRadius = 20.dp,
                    )
                } else {
                    Modifier
                },
            )
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp, bottom = 8.dp)
                .align(Alignment.CenterHorizontally)
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.tier_detail_pool_unranked_count,
                    pool.items.size,
                    pool.items.size,
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AddChip(onClick = onAddClick)
        }
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates -> dragController.registerItemsRowBounds(pool.id, coordinates.boundsInRoot()) }
                .testTag(TierDetailTestTags.POOL_ITEMS),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(pool.items, key = { it.id }) { item ->
                DraggableTile(
                    item = item,
                    sourceTierId = pool.id,
                    width = 52.dp,
                    height = 76.dp,
                    dragController = dragController,
                    onMoveItem = onMoveItem,
                    onDeleteItem = onDeleteItem,
                    onDoubleTap = onDoubleTap,
                )
            }
        }
    }
}

@Composable
private fun AddChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .testTag(TierDetailTestTags.ADD_CHIP)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusIcon(18.dp, MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.tier_detail_add),
            style = TierYourLifeType.current.chipText,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
