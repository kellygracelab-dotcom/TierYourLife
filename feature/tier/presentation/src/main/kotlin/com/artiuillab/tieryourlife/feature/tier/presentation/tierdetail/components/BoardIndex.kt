package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.TierRibbon

internal val BOARD_INDEX_WIDTH: Dp = 320.dp

/**
 * Every board beside the open one, as text: boards somebody knows by name are
 * found faster by a line of type. The feed gets no such column -- there the
 * picture is the identifier. No chevron: selection is already shown.
 */
@Composable
internal fun BoardIndex(
    lists: List<TierList>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(BOARD_INDEX_WIDTH).testTag(TierDetailTestTags.BOARD_INDEX),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box {
            Column(Modifier.statusBarsPadding()) {
                Text(
                    text = stringResource(R.string.tier_lists_title),
                    modifier = Modifier.height(64.dp).padding(start = 16.dp, top = 18.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = pluralStringResource(R.plurals.tier_lists_count, lists.size, lists.size),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Its own scroll: choosing a board must not throw away where
                // somebody scrolled to.
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(lists, key = { it.id }) { list ->
                        IndexRow(
                            list = list,
                            selected = list.id == selectedId,
                            onSelect = { onSelect(list.id) },
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun IndexRow(list: TierList, selected: Boolean, onSelect: () -> Unit) {
    val ranked = list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
    val inPool = list.tiers.firstOrNull { it.isPool }?.items?.size ?: 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
            .testTag(TierDetailTestTags.indexRow(list.id)),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Column(
            modifier = if (selected) {
                Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            } else {
                Modifier.padding(14.dp)
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = list.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Text(
                text = stringResource(
                    R.string.tier_lists_card_ranked_and_in_pool,
                    pluralStringResource(R.plurals.tier_lists_ranked_count, ranked, ranked),
                    pluralStringResource(R.plurals.tier_lists_in_pool_count, inPool, inPool),
                ),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            TierRibbon(list.tiers)
        }
    }
}
