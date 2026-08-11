package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags

@Composable
internal fun ResultsList(
    items: List<CatalogueItem>,
    selectedIds: Set<String>,
    selectedTint: Color,
    onToggle: (CatalogueItem) -> Unit,
) {
    LazyColumn(modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_RESULTS_LIST)) {
        items(items, key = { it.id }) { item ->
            val isSelected = item.id in selectedIds
            val background = if (isSelected) selectedTint else Color.Transparent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .selectable(selected = isSelected, onClick = { onToggle(item) })
                    .testTag(CatalogueSearchTestTags.itemSearchResult(item.id))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(width = 44.dp, height = 64.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 16.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    item.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = TierYourLifeType.current.captionUnderTitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SelectionCheckbox(isSelected = isSelected)
            }
        }
    }
}
