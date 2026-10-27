package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.core.theme.ui.ChevronIcon
import com.artiuillab.tieryourlife.feature.tier.board.components.ListArt
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.core.theme.R as ThemeR
import com.artiuillab.tieryourlife.feature.tier.board.R as BoardR

/**
 * A board found by its name: its picture, the name, and how far along it is,
 * on two lines (§2.3). A search is read down a column of names, so the ribbon
 * and the star of the full card wait for the board itself.
 */
@Composable
internal fun SearchResultRow(list: TierList, onClick: () -> Unit) {
    val ranked = list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
    val inPool = list.tiers.firstOrNull { it.isPool }?.items?.size ?: 0
    val inPoolText = pluralStringResource(BoardR.plurals.tier_lists_in_pool_count, inPool, inPool)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 12.dp)
            .testTag("tier_list_card_${list.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ListArt(
            coverImageUrl = list.coverImageUrl,
            previewImages = list.coverImageUrls,
            tierColors = list.tiers.filterNot { it.isPool }.map { it.colorLight },
            modifier = Modifier
                .size(THUMBNAIL)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = list.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (ranked == 0) {
                    stringResource(R.string.tier_lists_card_nothing_ranked_yet, inPoolText)
                } else {
                    stringResource(
                        BoardR.string.tier_lists_card_ranked_and_in_pool,
                        pluralStringResource(ThemeR.plurals.tier_lists_ranked_count, ranked, ranked),
                        inPoolText,
                    )
                },
                style = TierYourLifeType.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ChevronIcon()
    }
}

private val THUMBNAIL = 44.dp
