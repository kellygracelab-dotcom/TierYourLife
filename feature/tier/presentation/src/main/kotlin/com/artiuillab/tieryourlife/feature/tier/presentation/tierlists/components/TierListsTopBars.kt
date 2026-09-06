package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.ui.ClearIcon
import com.artiuillab.tieryourlife.core.theme.ui.DeleteOutlineIcon
import com.artiuillab.tieryourlife.core.theme.ui.HomeIconButton
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

@Composable
internal fun HomeHeader(totalListCount: Int, rankedCount: Int) {
    // The counters head the boards below them, so they keep the boards' measure.
    CenteredContent(
        ContentWidth.Reading,
        Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
    ) {
        if (totalListCount > 0) {
            Text(
                text = stringResource(
                    R.string.tier_lists_summary,
                    pluralStringResource(ThemeR.plurals.tier_lists_count, totalListCount, totalListCount),
                    pluralStringResource(ThemeR.plurals.tier_lists_rankings_count, rankedCount, rankedCount),
                    stringResource(R.string.tier_lists_private),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SelectionTopBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .atMost(ContentWidth.Reading)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp)
                .testTag(TierListsTestTags.SELECTION_BAR),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeIconButton(
                stringResource(R.string.cd_close_selection),
                onClose,
                TierListsTestTags.SELECTION_CLOSE,
            ) { ClearIcon(24.dp, onSurfaceVariant) }
            Text(
                text = pluralStringResource(R.plurals.selection_count, count, count),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val deleteDescription = stringResource(
                R.string.cd_delete_selected,
                pluralStringResource(ThemeR.plurals.tier_lists_count, count, count),
            )
            HomeIconButton(deleteDescription, onDelete, TierListsTestTags.SELECTION_DELETE) {
                DeleteOutlineIcon(24.dp, onSurfaceVariant)
            }
        }
    }
}
