package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags

private data class RankedEntry(val position: Int, val item: TierItem, val tier: Tier)

private fun buildRankedEntries(rankedTiers: List<Tier>): List<RankedEntry> {
    var rank = 0
    return rankedTiers.flatMap { tier -> tier.items.map { item -> RankedEntry(++rank, item, tier) } }
}

@Composable
internal fun RankedList(
    rankedTiers: List<Tier>,
    onSelect: (itemId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = remember(rankedTiers) { buildRankedEntries(rankedTiers) }

    Column(modifier.fillMaxWidth()) {
        val headerText = if (entries.isEmpty()) {
            stringResource(R.string.tier_detail_ranked_header_empty)
        } else {
            pluralStringResource(R.plurals.tier_detail_ranked_header, entries.size, entries.size)
        }
        Text(
            text = headerText,
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                .testTag(TierDetailTestTags.RANKED_HEADER),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(TierDetailTestTags.RANKED_LIST),
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.item.id }) { index, entry ->
                RankedItemRow(entry = entry, onSelect = onSelect)
                if (index < entries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RankedItemRow(entry: RankedEntry, onSelect: (itemId: Long) -> Unit) {
    val description = String.format(
        stringResource(R.string.tier_detail_ranked_row_description),
        entry.position,
        entry.item.title,
        entry.tier.label,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable { onSelect(entry.item.id) }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag(TierDetailTestTags.rankedRow(entry.item.id))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.position.toString(),
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
        )
        ItemTile(item = entry.item, width = 32.dp, height = 48.dp)
        Text(
            text = entry.item.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TierBadge(tier = entry.tier)
    }
}

@Composable
private fun TierBadge(tier: Tier) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    Box(
        modifier = Modifier
            .height(24.dp)
            .widthIn(min = 24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.band)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tier.label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onBand,
        )
    }
}

@Composable
internal fun RankedPoolSection(
    pool: Tier,
    onAddClick: () -> Unit,
    onGenerateClick: () -> Unit,
    onSelect: (itemId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val description = pluralStringResource(
        if (expanded) {
            R.plurals.tier_detail_ranked_pool_expanded_description
        } else {
            R.plurals.tier_detail_ranked_pool_collapsed_description
        },
        pool.items.size,
        pool.items.size,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .padding(top = 6.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .align(Alignment.CenterHorizontally)
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics(mergeDescendants = true) { contentDescription = description }
                .testTag(TierDetailTestTags.RANKED_POOL_COLLAPSED)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.rotate(if (expanded) 180f else 0f)) {
                ExpandLessIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = pluralStringResource(
                    R.plurals.tier_detail_pool_unranked_count,
                    pool.items.size,
                    pool.items.size,
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            CollapsedPoolAddChip(onClick = onAddClick)
            Spacer(Modifier.width(8.dp))
            GenerateChip(onClick = onGenerateClick)
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag(TierDetailTestTags.RANKED_POOL_ITEMS),
            ) {
                pool.items.forEachIndexed { index, item ->
                    PoolItemRow(item = item, onSelect = onSelect)
                    if (index < pool.items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PoolItemRow(item: TierItem, onSelect: (itemId: Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable { onSelect(item.id) }
            .semantics(mergeDescendants = true) { contentDescription = item.title }
            .testTag(TierDetailTestTags.rankedPoolItem(item.id))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemTile(item = item, width = 32.dp, height = 48.dp)
        Text(
            text = item.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CollapsedPoolAddChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .testTag(TierDetailTestTags.ADD_CHIP)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusIcon(18.dp, MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.tier_detail_add),
            style = TierYourLifeType.current.chipText,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RankedListLightPreview() = TierYourLifeTheme(false) {
    RankedList(rankedTiers = previewTierList.tiers.filterNot { it.isPool }, onSelect = {})
}

@Preview(showBackground = true)
@Composable
private fun RankedListDarkPreview() = TierYourLifeTheme(true) {
    RankedList(rankedTiers = previewTierList.tiers.filterNot { it.isPool }, onSelect = {})
}

@Preview(showBackground = true)
@Composable
private fun RankedPoolSectionLightPreview() = TierYourLifeTheme(false) {
    RankedPoolSection(
        pool = previewTierList.tiers.first { it.isPool },
        onAddClick = {},
        onGenerateClick = {},
        onSelect = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun RankedPoolSectionDarkPreview() = TierYourLifeTheme(true) {
    RankedPoolSection(
        pool = previewTierList.tiers.first { it.isPool },
        onAddClick = {},
        onGenerateClick = {},
        onSelect = {},
    )
}
