package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

private val RANK_IT_LINE_HEIGHT = 44.dp

/** The list this one was changed from, a tap away; by nobody when the original is anonymous. */
@Composable
internal fun BasedOnLine(basedOn: BasedOn, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = if (basedOn.authorName.isBlank()) {
            stringResource(R.string.community_based_on_nameless, basedOn.title)
        } else {
            stringResource(R.string.community_based_on, basedOn.title, basedOn.authorName)
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(CommunityListTestTags.BASED_ON),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** What a folded, empty pool says on Theirs: the cards are there to be moved. */
@Composable
internal fun RankItYourselfLine(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RANK_IT_LINE_HEIGHT)
            .testTag(CommunityListTestTags.RANK_IT_YOURSELF),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.community_rank_it_yourself),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.community_drag_any_card),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** The cards whose picture's license asks for a name, in the author's order. */
internal fun TierList.creditedCards(): List<TierItem> = tiers.flatMap { it.items }.filter { it.credit != null }

/** Under the last row, quiet: who each credited picture is by, as its license asks. */
@Composable
internal fun PictureCredits(cards: List<TierItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 20.dp, end = 8.dp, bottom = 8.dp)
            .testTag(CommunityListTestTags.PICTURE_CREDITS),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.community_picture_credits),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        cards.forEach { card ->
            Text(
                text = stringResource(R.string.community_picture_credit_line, card.title, card.credit.orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
