package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
 * The board's own scope comes first and "All" after it, and the order holds
 * still while the person moves between them.
 */
@Composable
internal fun ScopeChips(
    first: PictureScope,
    selected: PictureScope,
    onSelect: (PictureScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = remember(first) { listOf(first) + PictureScope.entries.filter { it != first } }
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
private fun PictureScope.label(): String = when (this) {
    PictureScope.All -> stringResource(R.string.item_search_scope_all)
    PictureScope.Animals -> stringResource(R.string.item_search_scope_animals)
    else -> ListCategory.fromId(id)?.let { stringResource(it.labelRes) } ?: stringResource(R.string.item_search_scope_all)
}
