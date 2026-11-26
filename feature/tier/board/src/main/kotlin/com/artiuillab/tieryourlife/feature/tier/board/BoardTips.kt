package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.artiuillab.tieryourlife.core.theme.tips.LocalTipMemory
import com.artiuillab.tieryourlife.core.theme.tips.ShownTip
import com.artiuillab.tieryourlife.core.theme.tips.Tip
import com.artiuillab.tieryourlife.core.theme.tips.TipHost
import com.artiuillab.tieryourlife.core.theme.tips.TipMemory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Below this, placing by hand is quick and Play is not worth a word (artboard 48). */
internal const val PLAY_TIP_FROM = 8

/**
 * Which tip a board shows now, if any: each once, one at a time, and never to someone who has
 * already done what it explains. A reader looking at the author's own ranking gets none.
 */
internal fun TipMemory.tipFor(list: TierList, readOnly: Boolean, canAdd: Boolean, canPlay: Boolean): Tip? {
    if (off || readOnly) return null
    val cards = list.tiers.sumOf { it.items.size }
    val inPool = list.tiers.filter { it.isPool }.sumOf { it.items.size }
    val placed = cards - inPool
    return when {
        canAdd && cards == 0 && !seen(Tip.Add) -> Tip.Add
        inPool > 0 && placed == 0 && !seen(Tip.Place) -> Tip.Place
        canPlay && placed > 0 && cards >= PLAY_TIP_FROM && seen(Tip.Place) && !seen(Tip.Play) -> Tip.Play
        else -> null
    }
}

/**
 * The board's tips over [content], which gets [actions] back with what the tips listen for:
 * a card put in a tier, Play opened, Add tapped. Doing it is as good as being told.
 */
@Composable
internal fun BoardTips(
    list: TierList,
    actions: TierDetailActions,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (TierDetailActions) -> Unit,
) {
    val memory = LocalTipMemory.current
    val current by rememberUpdatedState(list)
    val heard = remember(actions, memory) {
        actions.copy(
            onMoveItem = { itemId, toTierId, toPosition ->
                if (current.tiers.any { it.id == toTierId && !it.isPool }) memory.markSeen(Tip.Place)
                actions.onMoveItem(itemId, toTierId, toPosition)
            },
            onPlay = actions.onPlay?.let { play ->
                {
                    memory.markSeen(Tip.Play)
                    play()
                }
            },
            onAddClick = actions.onAddClick?.let { add ->
                {
                    memory.markSeen(Tip.Add)
                    add()
                }
            },
        )
    }
    val tip = memory.tipFor(list, readOnly, canAdd = actions.onAddClick != null, canPlay = actions.onPlay != null)
    TipHost(
        shown = tip?.let { ShownTip(it, tipText(it, list.tiers.sumOf { tier -> tier.items.size })) },
        onClose = { tip?.let(memory::markSeen) },
        onNoMore = memory::turnOff,
        modifier = modifier,
    ) { content(heard) }
}

@Composable
private fun tipText(tip: Tip, cards: Int): AnnotatedString = when (tip) {
    Tip.Place -> AnnotatedString.fromHtml(stringResource(R.string.tip_place))
    Tip.Play -> AnnotatedString.fromHtml(pluralStringResource(R.plurals.tip_play, cards, cards))
    Tip.Add -> AnnotatedString(stringResource(R.string.tip_add))
}
