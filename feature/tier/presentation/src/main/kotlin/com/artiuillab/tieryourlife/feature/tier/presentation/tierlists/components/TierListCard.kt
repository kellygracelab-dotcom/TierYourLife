package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import android.text.format.DateUtils
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CheckIcon

private val COVER_THUMBNAIL = 56.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun editedLine(arrivedFrom: String?, editedAt: Long): String {
    val ago = DateUtils.getRelativeTimeSpanString(
        editedAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    return when {
        arrivedFrom != null -> stringResource(R.string.conflict_edited_there, arrivedFrom, ago)
        else -> stringResource(R.string.conflict_edited_here, ago)
    }
}

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
            if (list.coverImageUrl != null) {
                // Own lists are known by their name; the cover is a reminder,
                // not the whole card.
                AsyncImage(
                    model = list.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(COVER_THUMBNAIL)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        list.title,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Only on the copy, and only while its twin is still here:
                    // two boards with one name is the state this answers, and
                    // once one of them is gone the tag is noise.
                    if (list.hasTwin && list.arrivedFrom != null) {
                        Spacer(Modifier.width(8.dp))
                        ArrivedFromChip(list.arrivedFrom)
                    }
                }
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
                    style = TierYourLifeType.current.supportingLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Which of the two was worked on more recently is the thing
                // that actually tells them apart; the phone's name only helps
                // when it happens to be a human one.
                val editedAt = list.editedAt
                if (list.hasTwin && editedAt != null) {
                    Text(
                        text = editedLine(list.arrivedFrom, editedAt),
                        style = TierYourLifeType.current.supportingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (selectionMode) {
                SelectionCheckbox(selected)
            } else {
                ChevronIcon()
            }
        }
        TierRibbon(list.tiers)
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
internal fun TierRibbon(tiers: List<Tier>) {
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
