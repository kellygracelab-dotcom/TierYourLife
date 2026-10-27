package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.CoverOpenIcon
import com.artiuillab.tieryourlife.core.theme.ui.CoverType
import com.artiuillab.tieryourlife.feature.tier.board.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.presentation.R

/**
 * A card's peek (§5.3): bigger, with its tier and place, and the way to it on
 * the main screen. A sheet inside the cover, not the system's: nothing here
 * is dragged, so there is no handle to drag it by.
 */
@Composable
internal fun CoverPeek(
    item: TierItem?,
    tier: Tier?,
    onDismiss: () -> Unit,
    onOpenMain: (item: TierItem, tierId: Long) -> Unit,
) {
    // The last one shown stays drawn while the sheet leaves.
    var shown by remember { mutableStateOf<Pair<TierItem, Tier>?>(null) }
    if (item != null && tier != null) shown = item to tier
    val visible = item != null && tier != null
    BackHandler(enabled = visible, onBack = onDismiss)

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(PEEK_MILLIS)), exit = fadeOut(tween(PEEK_MILLIS))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(coverColors.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(PEEK_MILLIS)) { it } + fadeIn(tween(PEEK_MILLIS)),
            exit = slideOutVertically(tween(PEEK_MILLIS)) { it } + fadeOut(tween(PEEK_MILLIS)),
        ) {
            shown?.let { (card, of) -> PeekSheet(card, of, onOpenMain) }
        }
    }
}

@Composable
private fun PeekSheet(item: TierItem, tier: Tier, onOpenMain: (item: TierItem, tierId: Long) -> Unit) {
    val place = tier.items.indexOfFirst { it.id == item.id } + 1
    val count = tier.items.size
    val caption = tier.caption?.takeIf { it.isNotBlank() }
    val position = if (caption != null) {
        stringResource(R.string.cover_card_position, caption, place, count)
    } else {
        stringResource(R.string.cover_card_position_plain, place, count)
    }
    val band = tierRowColors(tier.colorLight, tier.colorDark)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(coverColors.sheet)
            // The sheet itself takes taps, so one on it does not reach the scrim.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp)
            .testTag(CoverTestTags.PEEK),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverCard(item, onClick = null, width = 60.dp, height = 86.dp, corner = 8.dp, textStyle = CoverListType.PeekCardText)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, style = CoverListType.PeekTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .defaultMinSize(minWidth = 20.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(band.band)
                            .padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(tier.label, style = CoverListType.Chip, color = band.onBand, maxLines = 1) }
                    Text(position, style = CoverListType.PeekMeta, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        FilledTonalButton(
            onClick = { onOpenMain(item, tier.id) },
            modifier = Modifier.padding(top = 14.dp).fillMaxWidth().height(40.dp).testTag(CoverTestTags.PEEK_OPEN_MAIN),
        ) {
            CoverOpenIcon(18.dp, MaterialTheme.colorScheme.onSecondaryContainer)
            Text(stringResource(R.string.cover_open_main), modifier = Modifier.padding(start = 8.dp), style = CoverType.Button)
        }
    }
}

private const val PEEK_MILLIS = 180
