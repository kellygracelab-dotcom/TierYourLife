package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags

private const val LOAD_MORE_CELLS_AHEAD = 9

/** Picture search shows pictures: a grid of them, each with its title, several chosen at once. */
@Composable
internal fun ResultsGrid(
    items: List<CatalogueItem>,
    selectedIds: Set<String>,
    onToggle: (CatalogueItem) -> Unit,
    loadingMore: Boolean = false,
    onNearEnd: () -> Unit = {},
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, items.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                if (lastVisible >= items.size - LOAD_MORE_CELLS_AHEAD) onNearEnd()
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = CELL_MIN_WIDTH),
        state = gridState,
        modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_RESULTS_LIST),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items, key = { it.id }) { item ->
            ResultCell(item = item, selected = item.id in selectedIds, onToggle = { onToggle(item) })
        }
        if (loadingMore) {
            item(key = CatalogueSearchTestTags.ITEM_SEARCH_LOADING_MORE, span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CatalogueSearchTestTags.ITEM_SEARCH_LOADING_MORE)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultCell(item: CatalogueItem, selected: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .selectable(selected = selected, onClick = onToggle)
            .testTag(CatalogueSearchTestTags.itemSearchResult(item.id)),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PICTURE_ASPECT)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(if (selected) Modifier.border(3.dp, primary, shape) else Modifier),
        ) {
            AsyncImage(
                model = item.thumbUrl ?: item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .background(primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CheckIcon(14.dp, MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Column {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = TierYourLifeType.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private const val PICTURE_ASPECT = 3f / 4f
private val CELL_MIN_WIDTH = 104.dp
