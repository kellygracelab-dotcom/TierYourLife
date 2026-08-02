package com.artiuillab.tieryourlife.feature.tier.presentation.ui.tierlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.ui.color.tierRowColors

@Composable
internal fun TierListCard(list: TierList, onTierListClick: (Long) -> Unit) {
    val ranked = list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
    val inPool = list.tiers.firstOrNull { it.isPool }?.items?.size ?: 0
    val rankedText = pluralStringResource(R.plurals.tier_lists_ranked_count, ranked, ranked)
    val inPoolText = pluralStringResource(R.plurals.tier_lists_in_pool_count, inPool, inPool)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .testTag("tier_list_card_${list.id}")
            .clickable { if (list.id > 0) onTierListClick(list.id) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    list.title,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (ranked == 0) {
                        stringResource(R.string.tier_lists_card_nothing_ranked_yet, inPoolText)
                    } else {
                        stringResource(
                            R.string.tier_lists_card_ranked_and_in_pool,
                            rankedText,
                            inPoolText,
                        )
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ChevronIcon()
        }
        TierRibbon(list.tiers)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val status = tierListStatus(list.title, ranked)
            status.icon()
            Spacer(Modifier.width(8.dp))
            Text(
                text = status.label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TierRibbon(tiers: List<Tier>) {
    val rankedTiers = tiers.filterNot { it.isPool }
    val pool = tiers.firstOrNull { it.isPool }
    val total = tiers.sumOf { it.items.size }
    val media = TierYourLifeMedia.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (total == 0) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(media.unrankedRibbon),
            )
        } else {
            rankedTiers.filter { it.items.isNotEmpty() }.forEachIndexed { index, tier ->
                val band = tierRowColors(tier.colorLight, tier.colorDark).band
                Box(
                    Modifier
                        .weight(tier.items.size.toFloat())
                        .height(8.dp)
                        .clip(
                            if (index == 0) RoundedCornerShape(
                                topStart = 4.dp,
                                bottomStart = 4.dp,
                            ) else RoundedCornerShape(0.dp),
                        )
                        .background(band),
                )
            }
            if (pool != null && pool.items.isNotEmpty()) {
                Box(
                    Modifier
                        .weight(pool.items.size.toFloat())
                        .height(8.dp)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(media.unrankedRibbon),
                )
            }
        }
    }
}

private data class TierListStatus(
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
private fun tierListStatus(title: String, ranked: Int): TierListStatus = when (title) {
    "Sci-fi films" -> TierListStatus(
        label = stringResource(R.string.tier_lists_status_sci_fi),
        icon = { HistoryIcon() },
    )

    "Every A24 film" -> TierListStatus(
        label = stringResource(R.string.tier_lists_status_a24),
        icon = { ScheduleIcon() },
    )

    else -> if (ranked == 0) {
        TierListStatus(stringResource(R.string.tier_lists_status_start_dragging)) { DragIcon() }
    } else {
        TierListStatus(stringResource(R.string.tier_lists_status_updated_recently)) { HistoryIcon() }
    }
}
