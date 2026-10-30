package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
