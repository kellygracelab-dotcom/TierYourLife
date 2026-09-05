package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardFilters
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardSort
import com.artiuillab.tieryourlife.feature.tier.domain.lists.PublishedFilter
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.ClearIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

private val ROW_HEIGHT = 52.dp

/** Wide enough to show the strip continues, narrow enough not to eat a chip. */
private val FADE_WIDTH = 24.dp

/**
 * The sort is out here and the filters behind a button: a sort is undone at
 * a glance and its value is worth reading; a filter changes what is on
 * screen, which the screen already shows, and the ones that are on come back
 * as chips. Only the chips may scroll out of reach.
 */
@Composable
internal fun BoardControlsRow(
    sort: BoardSort,
    filters: BoardFilters,
    asPictures: Boolean,
    onSelectSort: (BoardSort) -> Unit,
    onOpenFilters: () -> Unit,
    onClearCategory: () -> Unit,
    onClearPublished: () -> Unit,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .padding(horizontal = 16.dp)
            .testTag(TierListsTestTags.BOARD_CONTROLS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            LazyRow(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "sort") { SortChip(sort, onSelectSort) }
                items(filters.applied(), key = { it.key }) { applied ->
                    AppliedFilterChip(applied, onClearCategory, onClearPublished)
                }
            }
            // Only where something can actually be under it, so the row does
            // not look cut off when there is nothing beyond the edge.
            if (filters.any) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(FADE_WIDTH)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                            ),
                        ),
                )
            }
        }

        HomeIconButton(
            contentDescription = stringResource(R.string.lists_filter),
            onClick = onOpenFilters,
            testTag = TierListsTestTags.BOARD_FILTER_BUTTON,
        ) {
            FilterIcon(on = filters.any)
        }
        HomeIconButton(
            contentDescription = stringResource(
                if (asPictures) {
                    R.string.tier_lists_content_description_as_rows
                } else {
                    R.string.tier_lists_content_description_as_pictures
                },
            ),
            onClick = onToggleView,
            testTag = TierListsTestTags.VIEW_TOGGLE,
        ) {
            if (asPictures) RowsIcon() else PicturesIcon()
        }
    }
}

@Composable
private fun SortChip(sort: BoardSort, onSelectSort: (BoardSort) -> Unit) {
    var open by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = false,
            onClick = { open = true },
            label = { Text(stringResource(sort.labelRes)) },
            trailingIcon = { ChevronDownIcon() },
            modifier = Modifier.testTag(TierListsTestTags.BOARD_SORT),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            BoardSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        open = false
                        onSelectSort(option)
                    },
                    modifier = Modifier.testTag(TierListsTestTags.boardSortOption(option)),
                )
            }
        }
    }
}

@Composable
private fun AppliedFilterChip(
    applied: AppliedFilter,
    onClearCategory: () -> Unit,
    onClearPublished: () -> Unit,
) {
    FilterChip(
        selected = true,
        onClick = if (applied.key == CATEGORY_KEY) onClearCategory else onClearPublished,
        label = { Text(stringResource(applied.labelRes)) },
        trailingIcon = { ClearIcon(18.dp, MaterialTheme.colorScheme.onSecondaryContainer) },
        modifier = Modifier.testTag(TierListsTestTags.appliedFilter(applied.key)),
    )
}

private const val CATEGORY_KEY = "category"
private const val PUBLISHED_KEY = "published"

private data class AppliedFilter(val key: String, val labelRes: Int)

/** The filters that are on, as the chips that undo them. */
private fun BoardFilters.applied(): List<AppliedFilter> = buildList {
    category?.let { add(AppliedFilter(CATEGORY_KEY, it.labelRes)) }
    published?.let {
        add(
            AppliedFilter(
                PUBLISHED_KEY,
                if (it == PublishedFilter.Public) R.string.lists_filter_public else R.string.lists_filter_private,
            ),
        )
    }
}

internal val BoardSort.labelRes: Int
    get() = when (this) {
        BoardSort.Newest -> R.string.lists_sort_newest
        BoardSort.Oldest -> R.string.lists_sort_oldest
    }
