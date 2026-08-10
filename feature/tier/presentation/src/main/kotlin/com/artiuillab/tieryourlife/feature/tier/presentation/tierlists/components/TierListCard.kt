package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CheckIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TierListCard(
    list: TierList,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
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
            // Selection mode re-binds what a tap does, so the card's own semantics
            // must say "checkbox" while it's active — TalkBack otherwise has no way to
            // know a tap now selects rather than opens (docs/design-spec-home.md,
            // section 11). Applied before combinedClickable so it lands on the same
            // merged semantics node as the click/long-click actions, not a separate one.
            .then(
                if (selectionMode) {
                    Modifier.semantics {
                        role = Role.Checkbox
                        toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
                    }
                } else {
                    Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    list.title,
                    style = MaterialTheme.typography.bodyLarge,
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
                    style = TierYourLifeType.current.captionUnderTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selectionMode) {
                SelectionCheckbox(selected)
            } else {
                ChevronIcon()
            }
        }
        TierRibbon(list.tiers)
        // The app holds no update-timestamp data, so a list with rankings shows no
        // footnote at all rather than a claim it can't back — only the "nothing ranked
        // yet" case has anything true to say here. Cards differ in height as a result;
        // that's intended (see docs/design-spec-home.md, section 1).
        if (ranked == 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DragIcon()
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tier_lists_status_start_dragging),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Hand-drawn to an exact 24dp/4dp-radius box rather than the stock M3 Checkbox, the
// same call CatalogueSearchScreen's SelectionCheckbox already made for the search sheet
// (docs/design-spec-home.md, section 11). Not independently focusable or announced —
// the checkbox state lives on the card's own semantics (see TierListCard above), so
// this box clears its own to avoid TalkBack reading "checkbox" twice for one card.
@Composable
private fun SelectionCheckbox(selected: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (selected) {
                    Modifier.background(primary)
                } else {
                    Modifier.border(2.dp, outline, RoundedCornerShape(4.dp))
                },
            )
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            CheckIcon(18.dp, onPrimary)
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

