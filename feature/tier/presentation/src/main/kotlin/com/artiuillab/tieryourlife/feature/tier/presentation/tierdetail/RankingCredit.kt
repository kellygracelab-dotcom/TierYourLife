package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.ui.ClearIcon
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R

/** The id of the Community list this board is a copy of, if it is one. */
internal fun TierList.original(): String? = rankingOf ?: takenFrom

/** [list]'s credit line when it is somebody's copy and the line was not put away on this device. */
@Composable
internal fun creditOf(list: TierList?, onOpenOriginal: (String) -> Unit): (@Composable () -> Unit)? {
    val credits: RankingCreditViewModel = hiltViewModel()
    val putAway by credits.putAway.collectAsStateWithLifecycle()
    val original = list?.original() ?: return null
    if (list.uid in putAway) return null
    return {
        RankingCredit(
            authorName = list.authorName.orEmpty(),
            onOpen = { onOpenOriginal(original) },
            onPutAway = { credits.putAway(list.uid) },
        )
    }
}

/**
 * Under the title of a copy: where its list is, whose it is when the author
 * has a public name, and the way there. Put away, it stays away on this
 * device; the list settings still open the original.
 */
@Composable
internal fun RankingCredit(authorName: String, onOpen: () -> Unit, onPutAway: () -> Unit, modifier: Modifier = Modifier) {
    val putAway = stringResource(R.string.tier_detail_put_credit_away)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (authorName.isNotBlank()) {
                stringResource(R.string.tier_detail_ranking_of, authorName.trim())
            } else {
                stringResource(R.string.tier_detail_ranking_of_community)
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onOpen) { Text(stringResource(R.string.tier_detail_open_original)) }
        IconButton(onClick = onPutAway) {
            Box(Modifier.semantics { contentDescription = putAway }) {
                ClearIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
