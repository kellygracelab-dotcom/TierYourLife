package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.ui.CardLabel
import com.artiuillab.tieryourlife.core.theme.ui.CoverHeader
import com.artiuillab.tieryourlife.core.theme.ui.CoverIconButton
import com.artiuillab.tieryourlife.core.theme.ui.CoverOpenIcon
import com.artiuillab.tieryourlife.core.theme.ui.CoverTarget
import com.artiuillab.tieryourlife.core.theme.ui.CoverType
import com.artiuillab.tieryourlife.core.theme.ui.ExpandLessIcon
import com.artiuillab.tieryourlife.feature.tier.board.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/**
 * A list on the cover (§5): every tier, each on one line with "+N" for the
 * rest, empty ones kept small so the board's shape still shows. A tap opens
 * one row in full; a card shows a peek; ranking goes to the main screen.
 */
@Composable
fun CoverListScreen(
    list: TierList,
    next: TierList?,
    onBack: () -> Unit,
    onRankOnMain: (tierId: Long?) -> Unit,
    onCardOnMain: (item: TierItem, tierId: Long) -> Unit,
    onNext: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A new list starts at its top with nothing open, as if opened afresh.
    key(list.id) {
        var openTier by rememberSaveable { mutableStateOf<Long?>(null) }
        var peeking by rememberSaveable { mutableStateOf<Long?>(null) }
        val listState = rememberLazyListState()
        val tiers = list.tiers.filterNot { it.isPool }
        val ranked = tiers.sumOf { it.items.size }

        Box(modifier.fillMaxSize().testTag(CoverTestTags.LIST)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "header") {
                    CoverHeader(
                        title = list.title,
                        onBack = onBack,
                        count = pluralStringResource(ThemeR.plurals.tier_lists_ranked_count, ranked, ranked),
                        actions = {
                            CoverIconButton(
                                onClick = {
                                    val inView = tiers.getOrNull((listState.firstVisibleItemIndex - 1).coerceAtLeast(0))
                                    onRankOnMain(openTier ?: inView?.id)
                                },
                                description = stringResource(R.string.cover_open_main),
                                testTag = CoverTestTags.OPEN_LIST_ON_MAIN,
                            ) { CoverOpenIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
                        },
                    )
                }
                items(tiers, key = { it.id }) { tier ->
                    Box(Modifier.padding(horizontal = ROW_SIDE)) {
                        CoverTierRow(
                            tier = tier,
                            open = openTier == tier.id,
                            onToggle = { openTier = toggledRow(openTier, tier.id) },
                            onCard = { item -> peeking = item.id },
                        )
                    }
                }
                item(key = "end") {
                    EndOfList(next = next, onRankOnMain = { onRankOnMain(openTier ?: tiers.firstOrNull()?.id) }, onNext = onNext)
                }
            }

            val peekedTier = tiers.firstOrNull { tier -> tier.items.any { it.id == peeking } }
            val peekedItem = peekedTier?.items?.firstOrNull { it.id == peeking }
            CoverPeek(
                item = peekedItem,
                tier = peekedTier,
                onDismiss = { peeking = null },
                onOpenMain = { item, tierId ->
                    peeking = null
                    onCardOnMain(item, tierId)
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverTierRow(tier: Tier, open: Boolean, onToggle: () -> Unit, onCard: (TierItem) -> Unit) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    if (tier.items.isEmpty()) {
        EmptyTierRow(tier)
        return
    }
    val ring = coverColors.ring
    val cardCount = pluralStringResource(R.plurals.cover_cards_count, tier.items.size, tier.items.size)
    val hint = stringResource(if (open) R.string.cover_tap_fold else R.string.cover_tap_show_all)
    val description = listOf(tier.label, tier.caption.orEmpty(), cardCount, hint).filter { it.isNotBlank() }.joinToString(", ")
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .animateContentSize(tween(OPEN_MILLIS))
            .clip(RoundedCornerShape(10.dp))
            .background(colors.rowTint)
            .drawBehind { drawRect(colors.band, size = Size(COVER_BAND_DP.dp.toPx(), size.height)) }
            .let { if (open) it.border(2.dp, ring, RoundedCornerShape(10.dp)) else it }
            .clickable(onClick = onToggle)
            .semantics { contentDescription = description }
            .testTag(CoverTestTags.tierRow(tier.id)),
    ) {
        val plan = coverRowPlan(tier.items.size, coverCardsPerLine(maxWidth.value.toInt()), open)
        Box(Modifier.matchParentSize()) {
            Column(
                modifier = Modifier
                    .width(COVER_BAND_DP.dp)
                    .fillMaxHeight()
                    .padding(top = if (open) 10.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (open) Arrangement.Top else Arrangement.Center,
            ) {
                Text(tier.label, style = CoverListType.BandLetter, color = colors.onBand, maxLines = 1)
                if (open) ExpandLessIcon(16.dp, colors.onBand)
            }
        }
        FlowRow(
            modifier = Modifier.padding(start = COVER_BAND_DP.dp).padding(COVER_ROW_PADDING_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(COVER_CARD_GAP_DP.dp),
            verticalArrangement = Arrangement.spacedBy(COVER_CARD_GAP_DP.dp),
        ) {
            tier.items.take(plan.shown).forEach { item -> CoverCard(item, onClick = { onCard(item) }) }
            if (plan.more > 0) MoreTile(tier.id, plan.more, onToggle)
        }
    }
}

@Composable
private fun EmptyTierRow(tier: Tier) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.rowTint)
            .testTag(CoverTestTags.tierRow(tier.id)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(COVER_BAND_DP.dp).fillMaxHeight().background(colors.band), contentAlignment = Alignment.Center) {
            Text(tier.label, style = CoverListType.BandLetterEmpty, color = colors.onBand, maxLines = 1)
        }
        Text(
            stringResource(R.string.cover_tier_empty),
            modifier = Modifier.padding(horizontal = 10.dp),
            style = CoverListType.EmptyLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
}

/** A card at the cover's size: its picture, or its name broken only between words. */
@Composable
internal fun CoverCard(
    item: TierItem,
    onClick: (() -> Unit)?,
    width: Dp = COVER_CARD_WIDTH_DP.dp,
    height: Dp = CARD_HEIGHT,
    corner: Dp = 5.dp,
    textStyle: TextStyle = CoverListType.CardText,
) {
    val colors = coverColors
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(shape)
            .background(colors.card)
            .let { if (item.imageUrl == null) it.border(1.dp, colors.cardBorder, shape) else it }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .testTag(CoverTestTags.card(item.id)),
        contentAlignment = Alignment.Center,
    ) {
        if (item.imageUrl != null) {
            AsyncImage(model = item.imageUrl, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CardLabel(
                text = item.title,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                modifier = Modifier.padding(2.dp),
                smallest = 5.5.sp,
            )
        }
    }
}

@Composable
private fun MoreTile(tierId: Long, more: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(COVER_CARD_WIDTH_DP.dp, CARD_HEIGHT)
            .clip(RoundedCornerShape(5.dp))
            .background(coverColors.sheet)
            .clickable(onClick = onClick)
            .testTag(CoverTestTags.more(tierId)),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.cover_more, more), style = CoverListType.PlusN, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EndOfList(next: TierList?, onRankOnMain: () -> Unit, onNext: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = ROW_SIDE, end = ROW_SIDE, top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
            onClick = onRankOnMain,
            modifier = Modifier.fillMaxWidth().height(40.dp).testTag(CoverTestTags.RANK_ON_MAIN),
        ) {
            CoverOpenIcon(18.dp, MaterialTheme.colorScheme.onSecondaryContainer)
            Text(stringResource(R.string.cover_rank_on_main), modifier = Modifier.padding(start = 8.dp), style = CoverType.Button)
        }
        if (next != null) NextListCard(next, onClick = { onNext(next.id) })
    }
}

@Composable
private fun NextListCard(next: TierList, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(coverColors.nextCard)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 6.dp)
            .testTag(CoverTestTags.NEXT_LIST),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CoverThumbnail(next)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.cover_next_list), style = CoverType.RowMeta, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(next.title, style = CoverType.RowTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(CoverTarget), contentAlignment = Alignment.Center) {
            ArrowDownwardIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val CARD_HEIGHT = 52.dp

/** Board rows stand 12dp in from the cover's edges; the header keeps its own padding. */
private val ROW_SIDE = 12.dp
private const val OPEN_MILLIS = 200
