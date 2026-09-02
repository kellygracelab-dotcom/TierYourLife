package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardFilters
import com.artiuillab.tieryourlife.feature.tier.domain.lists.PublishedFilter
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Which boards are shown, put away behind a button.
 *
 * Applied at once rather than behind an Apply button: there are two questions
 * here and both are undone by tapping the same chip again, so a step that
 * only says "yes, I meant it" earns nothing.
 *
 * Deliberately without a "favourites" value. Starred boards are already
 * pinned to the top; a filter that showed only them would contradict the
 * pinning the moment both were on.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun BoardFiltersSheet(
    filters: BoardFilters,
    onFilters: (BoardFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag(TierListsTestTags.BOARD_FILTER_SHEET)) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.lists_filters_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Section(stringResource(R.string.lists_filter_category))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(
                    label = stringResource(R.string.lists_filter_any),
                    selected = filters.category == null,
                    testTag = TierListsTestTags.filterCategory(null),
                ) { onFilters(filters.copy(category = null)) }
                ListCategory.entries.forEach { category ->
                    Choice(
                        label = stringResource(category.labelRes),
                        selected = filters.category == category,
                        testTag = TierListsTestTags.filterCategory(category),
                    ) { onFilters(filters.copy(category = category)) }
                }
            }

            Section(stringResource(R.string.lists_filter_visibility))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(
                    label = stringResource(R.string.lists_filter_any),
                    selected = filters.published == null,
                    testTag = TierListsTestTags.filterPublished(null),
                ) { onFilters(filters.copy(published = null)) }
                PublishedFilter.entries.forEach { published ->
                    Choice(
                        label = stringResource(
                            if (published == PublishedFilter.Public) {
                                R.string.lists_filter_public
                            } else {
                                R.string.lists_filter_private
                            },
                        ),
                        selected = filters.published == published,
                        testTag = TierListsTestTags.filterPublished(published),
                    ) { onFilters(filters.copy(published = published)) }
                }
            }

            // Only where there is something to clear: a disabled control is a
            // question the screen is asking and answering by itself.
            if (filters.any) {
                TextButton(
                    onClick = { onFilters(BoardFilters()) },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .wrapContentWidth(Alignment.Start),
                ) {
                    Text(stringResource(R.string.lists_filter_clear))
                }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, testTag: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.testTag(testTag),
    )
}
