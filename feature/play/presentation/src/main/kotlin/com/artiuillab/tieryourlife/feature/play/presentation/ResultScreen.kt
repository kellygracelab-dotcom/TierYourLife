package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.board.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.board.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.ItemTile

/** What the result screen's buttons do. */
internal data class ResultActions(
    val onApply: () -> Unit = {},
    val onRefine: () -> Unit = {},
    val onPlayAgain: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onQuit: () -> Unit = {},
)

/** Where the duels put the cards, read-only at the board's compact size, and what to do with it. */
@Composable
internal fun ResultScreen(result: ResultState, actions: ResultActions) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(PlayTestTags.RESULT),
        color = MaterialTheme.colorScheme.surface,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val short = WindowShape.isShort(maxHeight)
            val oneRow = maxWidth >= WindowShape.MediumFrom
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                PlayHeader(title = stringResource(result.kind.nameRes), line = null, short = short, onQuit = actions.onQuit)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        Modifier
                            .atMost(RESULT_WIDTH)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ResultHeading(result)
                        result.tiers.forEach { PreviewRow(it) }
                    }
                }
                ResultButtons(result = result, actions = actions, oneRow = oneRow)
            }
        }
    }
}

private val RESULT_WIDTH = 720.dp

@Composable
private fun ResultHeading(result: ResultState) {
    Column {
        Text(
            text = stringResource(R.string.play_your_result),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = pluralStringResource(R.plurals.play_from_duels, result.duels, result.duels),
            style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (result.thin) ThinBanner(result)
    if (result.dashed > 0) {
        Text(
            text = stringResource(R.string.play_dashed_legend),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** More than a quarter of the cards met fewer than two others: their places are said to be a guess. */
@Composable
private fun ThinBanner(result: ResultState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PlayTestTags.THIN),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            InfoIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (result.canRefine) {
                    pluralStringResource(R.plurals.play_thin, result.dashed, result.dashed, result.refineDuels)
                } else {
                    pluralStringResource(R.plurals.play_thin_refined, result.dashed, result.dashed)
                },
                style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** A tier as the board's compact rows draw it, without anything to drag. */
@Composable
private fun PreviewRow(tier: ResultTier) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = if (tier.cards.isEmpty()) EMPTY_ROW else 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.rowTint),
    ) {
        Box(
            Modifier
                .width(BAND_WIDTH)
                .fillMaxHeight()
                .background(colors.band),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tier.label,
                style = TierYourLifeType.current.tierBandLetter.merge(TextStyle(fontSize = 20.sp, lineHeight = 24.sp)),
                color = colors.onBand,
                maxLines = 1,
            )
        }
        if (tier.cards.isEmpty()) {
            Text(
                text = stringResource(R.string.play_empty_tier),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                Modifier
                    .weight(1f)
                    .padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val outline = MaterialTheme.colorScheme.outline
                tier.cards.forEach { card ->
                    Box(if (card.dashed) Modifier.dashedBorder(1.5.dp, outline, 6.dp, dashLength = 4.dp, gapLength = 3.dp) else Modifier) {
                        ItemTile(card.item, TILE_WIDTH, TILE_HEIGHT)
                    }
                }
            }
        }
    }
}

private val BAND_WIDTH = 64.dp
private val EMPTY_ROW = 40.dp
private val TILE_WIDTH = 40.dp
private val TILE_HEIGHT = 58.dp

/** Stacked full width on a phone, in the order of how likely each is; one row on anything wider. */
@Composable
private fun ResultButtons(result: ResultState, actions: ResultActions, oneRow: Boolean) {
    val apply: @Composable (Modifier) -> Unit = { modifier ->
        Button(onClick = actions.onApply, modifier = modifier.heightIn(min = 44.dp).testTag(PlayTestTags.APPLY)) {
            Text(stringResource(if (result.intoMine) R.string.play_put_in_mine else R.string.play_apply))
        }
    }
    val refine: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedButton(onClick = actions.onRefine, modifier = modifier.heightIn(min = 44.dp).testTag(PlayTestTags.REFINE)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.play_refine))
                Text(
                    text = pluralStringResource(R.plurals.play_refine_adds, result.refineDuels, result.refineDuels),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    val again: @Composable (Modifier) -> Unit = { modifier ->
        TextButton(onClick = actions.onPlayAgain, modifier = modifier.testTag(PlayTestTags.PLAY_AGAIN)) {
            Text(stringResource(R.string.play_again))
        }
    }
    val cancel: @Composable (Modifier) -> Unit = { modifier ->
        TextButton(onClick = actions.onCancel, modifier = modifier.testTag(PlayTestTags.CANCEL)) {
            Text(stringResource(R.string.play_cancel))
        }
    }
    val padding = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    if (oneRow) {
        Row(padding, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            cancel(Modifier)
            again(Modifier)
            if (result.canRefine) refine(Modifier)
            apply(Modifier)
        }
    } else {
        Column(padding, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            apply(Modifier.fillMaxWidth())
            if (result.canRefine) refine(Modifier.fillMaxWidth())
            again(Modifier.fillMaxWidth())
            cancel(Modifier.fillMaxWidth())
        }
    }
}
