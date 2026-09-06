package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.tier.board.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

/** Null is All, which is a filter state rather than a ninth category. */
@Composable
internal fun CategoryFilterRow(
    selected: ListCategory?,
    onSelect: (ListCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options: List<ListCategory?> = listOf(null) + ListCategory.entries

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CommunityTestTags.CATEGORY_FILTERS),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it?.id ?: "all" }) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = {
                    Text(stringResource(category?.labelRes ?: R.string.category_all))
                },
                leadingIcon = if (category == selected) {
                    { CheckIcon(18.dp, MaterialTheme.colorScheme.onSecondaryContainer) }
                } else {
                    null
                },
                modifier = Modifier.testTag(CommunityTestTags.categoryFilter(category)),
            )
        }
    }
}
