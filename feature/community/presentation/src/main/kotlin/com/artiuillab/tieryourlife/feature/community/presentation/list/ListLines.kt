package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.presentation.R

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

/** How many readers placed these cards their own way, and whose placing is on screen when it is not the author's. */
@Composable
internal fun RankingsLine(count: Int, chosen: Ranking?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = onOpen,
            label = { Text(pluralStringResource(R.plurals.community_rankings_count, count, count)) },
            modifier = Modifier.testTag(CommunityListTestTags.RANKINGS),
        )
        if (chosen != null) {
            Text(
                text = stringResource(R.string.community_showing_ranking_of, chosen.authorName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
