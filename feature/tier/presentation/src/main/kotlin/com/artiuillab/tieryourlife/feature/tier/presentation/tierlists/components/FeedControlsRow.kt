package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CheckIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Whose lists, and in what order, on one line.
 *
 * Three controls is one more than a feed carries, and the category row is
 * already the third. Source and order share this line because they are the two
 * that change together; the category answers a different question and keeps
 * its own row below.
 */
@Composable
internal fun FeedControlsRow(
    source: FeedSource,
    sort: FeedSort,
    onSelectSource: (FeedSource) -> Unit,
    onSelectSort: (FeedSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceChip(
            label = stringResource(R.string.feed_source_following),
            selected = source == FeedSource.Following,
            onClick = { onSelectSource(FeedSource.Following) },
            testTag = TierListsTestTags.FEED_SOURCE_FOLLOWING,
        )
        SourceChip(
            label = stringResource(R.string.feed_source_everyone),
            selected = source == FeedSource.Everyone,
            onClick = { onSelectSource(FeedSource.Everyone) },
            testTag = TierListsTestTags.FEED_SOURCE_EVERYONE,
        )
        SortMenu(sort = sort, onSelect = onSelectSort, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit, testTag: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { CheckIcon(18.dp, MaterialTheme.colorScheme.onSecondaryContainer) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(),
        modifier = Modifier.testTag(testTag),
    )
}

/**
 * The order, as a menu rather than two more chips.
 *
 * There are only two orders today, but they are not a choice of the same kind
 * as the source: the source is where you are, the order is how it is arranged.
 * Four chips in a row would read as four places to be.
 */
@Composable
private fun SortMenu(sort: FeedSort, onSelect: (FeedSort) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.cd_feed_sort)

    Row(modifier, horizontalArrangement = Arrangement.End) {
        FilterChip(
            selected = false,
            onClick = { open = true },
            label = { Text(stringResource(sort.label)) },
            trailingIcon = { ChevronDownIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .semantics { contentDescription = description }
                .testTag(TierListsTestTags.FEED_SORT),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FeedSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label)) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

private val FeedSort.label: Int
    get() = when (this) {
        FeedSort.Recent -> R.string.feed_sort_recent
        FeedSort.Popular -> R.string.feed_sort_popular
    }
