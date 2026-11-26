package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.feature.tier.board.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags

/**
 * What to search in, quietly: outlined chips, only the chosen one filled.
 * "All" comes first and the board's own scope after it, and the order holds
 * still while the person moves between them.
 */
@Composable
internal fun ScopeChips(
    first: PictureScope,
    selected: PictureScope,
    onSelect: (PictureScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = remember(first) {
        (listOf(PictureScope.All, first) + PictureScope.entries).distinct()
    }
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(order, key = { it.id }) { scope ->
            val chosen = scope == selected
            FilterChip(
                selected = chosen,
                onClick = { onSelect(scope) },
                label = { Text(scope.label()) },
                leadingIcon = if (chosen) {
                    { CheckIcon(FilterChipDefaults.IconSize, MaterialTheme.colorScheme.onSecondaryContainer) }
                } else {
                    null
                },
                modifier = Modifier.testTag(CatalogueSearchTestTags.itemSearchScope(scope.id)),
            )
        }
    }
}

@Composable
internal fun PictureScope.label(): String = when (this) {
    PictureScope.All -> stringResource(R.string.item_search_scope_all)
    PictureScope.Animals -> stringResource(R.string.item_search_scope_animals)
    else -> ListCategory.fromId(id)?.let { stringResource(it.labelRes) } ?: stringResource(R.string.item_search_scope_all)
}

/**
 * Over a section's results: that the search looked only there, and the way
 * out of it (walkthrough 26.11.2026). [more]: further pages, so the count is a floor.
 */
@Composable
internal fun ScopeLine(scope: PictureScope, count: Int, more: Boolean, onSearchEverywhere: () -> Unit) {
    val said = if (more) "$count+" else "$count"
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, end = 4.dp)
            .testTag(CatalogueSearchTestTags.ITEM_SEARCH_SCOPE_LINE),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.item_search_results_in, count, said, scope.label()),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
        TextButton(onClick = onSearchEverywhere, modifier = Modifier.testTag(CatalogueSearchTestTags.ITEM_SEARCH_EVERYWHERE)) {
            Text(stringResource(R.string.item_search_everywhere), maxLines = 1)
        }
    }
}
