package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.feature.tier.board.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.ItemTile
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRankings
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.core.theme.R as ThemeR
import com.artiuillab.tieryourlife.feature.tier.board.R as BoardR

/** How many tiers the window draws; the rest are counted under them. */
private const val TIERS_SHOWN = 3

/** How many of a tier's pictures stand beside its letter. */
private const val CARDS_SHOWN = 5

/**
 * The game's end (artboard 35): the result in a few lines over the board —
 * how many cards from how many duels, the first tiers with their cards — and
 * what to do with it: look at the list, share it as a picture, show it among
 * the list's rankings, or play again. The result is on the board already;
 * closing the window leaves it there, and Undo where it always is.
 */
@Composable
internal fun ReadySheet(
    ready: ReadyState,
    rankings: PlayRankings?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onAgain: () -> Unit,
) {
    AdaptiveSheet(onDismiss = onDismiss, width = SheetWidth.Choosing, modifier = Modifier.testTag(PlayTestTags.READY)) {
        ReadyContent(ready, rankings, onDismiss, onShare, onAgain, top = if (currentWindowShape.hasRail) 24.dp else 0.dp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReadyContent(
    ready: ReadyState,
    rankings: PlayRankings?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onAgain: () -> Unit,
    top: Dp = 0.dp,
) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = top, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.play_ready_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            val cards = pluralStringResource(ThemeR.plurals.list_items_count, ready.cards, ready.cards)
            val duels = pluralStringResource(R.plurals.play_estimate_duels, ready.duels, ready.duels)
            Text(
                text = stringResource(R.string.play_ready_summary, cards, duels),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(PlayTestTags.READY_SUMMARY),
            )
            Spacer(Modifier.height(16.dp))
            val filled = ready.board.tiers.filter { !it.isPool && it.items.isNotEmpty() }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filled.take(TIERS_SHOWN).forEach { ReadyTier(it) }
            }
            val more = filled.size - TIERS_SHOWN
            if (more > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = pluralStringResource(R.plurals.play_more_tiers, more, more),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(PlayTestTags.SEE_MY_LIST),
            ) { Text(stringResource(R.string.play_see_my_list)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(PlayTestTags.SHARE_RESULT),
            ) { Text(stringResource(BoardR.string.list_settings_share)) }
            Spacer(Modifier.height(4.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                if (rankings != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            rankings.onShow()
                        },
                        modifier = Modifier.testTag(PlayTestTags.READY_RANKINGS),
                    ) {
                        Text(stringResource(if (rankings.signedIn) ThemeR.string.ranking_offer_show else R.string.play_sign_in_to_show))
                    }
                }
                TextButton(onClick = onAgain, modifier = Modifier.testTag(PlayTestTags.PLAY_AGAIN)) {
                    Text(stringResource(R.string.play_play_again))
                }
            }
        }
}

/** A tier's letter on its colour, and its first pictures beside it. */
@Composable
private fun ReadyTier(tier: Tier) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(width = 40.dp, height = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.band),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tier.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBand,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
        tier.items.take(CARDS_SHOWN).forEach { item -> ItemTile(item = item, width = 36.dp, height = 52.dp, named = false) }
        val hidden = tier.items.size - CARDS_SHOWN
        if (hidden > 0) {
            Text(
                text = "+$hidden",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
