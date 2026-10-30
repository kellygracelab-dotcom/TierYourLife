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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.board.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.board.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.ItemTile
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierRowDensity

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
            // Sideways, and wide enough for a column beside the tiers; half a phone in split view is not.
            if (WindowShape.isShort(maxHeight) && maxWidth >= WindowShape.MediumFrom) {
                SideResult(result, actions)
            } else {
                UprightResult(result, actions, oneRow = maxWidth >= WindowShape.MediumFrom)
            }
        }
    }
}

/** The board's own compact rows: what is applied looks like what is previewed. */
private val Rows = TierRowDensity.Compact
private val RESULT_WIDTH = 720.dp

@Composable
private fun UprightResult(result: ResultState, actions: ResultActions, oneRow: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuitButton(actions.onQuit)
            Text(
                text = stringResource(R.string.play_your_result),
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = pluralStringResource(R.plurals.play_from_duels, result.duels, result.duels),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(start = 8.dp, end = 12.dp),
                style = SMALL_LINE,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResultNotes(result)
                PreviewRows(result, narrowBands = false)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        UprightButtons(result = result, actions = actions, oneRow = oneRow)
    }
}

/**
 * Held sideways: what the result means and what to do with it in a column at
 * the start, the tiers scrolling beside it where the width is.
 */
@Composable
private fun SideResult(result: ResultState, actions: ResultActions) {
    val vertical = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(SIDE_COLUMN)
                .fillMaxHeight()
                .windowInsetsPadding(vertical),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuitButton(actions.onQuit)
                    Column(Modifier.padding(start = 4.dp)) {
                        Text(
                            text = stringResource(R.string.play_your_result),
                            style = SIDE_TITLE,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.play_from_duels, result.duels, result.duels),
                            style = SMALL_LINE,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(
                    Modifier.padding(start = 12.dp, top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { ResultNotes(result) }
            }
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ApplyButton(result, actions.onApply, Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (result.canRefine) RefineButton(stringResource(R.string.play_refine), actions.onRefine, Modifier)
                    Spacer(Modifier.weight(1f))
                    PlayAgainButton(actions.onPlayAgain, Modifier)
                    CancelButton(actions.onCancel, Modifier)
                }
            }
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(vertical)
                .padding(12.dp),
        ) {
            // Past six tiers the bands give their width to the cards and keep only the letter.
            PreviewRows(result, narrowBands = result.tiers.size > WIDE_BAND_TIERS)
        }
    }
}

private val SIDE_COLUMN = 320.dp
private const val WIDE_BAND_TIERS = 6
private val SIDE_TITLE = TextStyle(fontSize = 20.sp, lineHeight = 26.sp)
private val SMALL_LINE = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)

@Composable
private fun ResultNotes(result: ResultState) {
    if (!result.canApply) {
        NoteBanner(stringResource(R.string.play_nothing_to_apply), Modifier.testTag(PlayTestTags.NOTHING_TO_APPLY))
        return
    }
    if (result.thin) ThinBanner(result)
    if (result.dashed > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp, 17.dp)
                    .dashedBorder(1.dp, MaterialTheme.colorScheme.outline, 3.dp, dashLength = 3.dp, gapLength = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.play_dashed_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** More than a quarter of the cards met fewer than two others: their places are said to be a guess. */
@Composable
private fun ThinBanner(result: ResultState) {
    NoteBanner(
        text = if (result.canRefine) {
            pluralStringResource(R.plurals.play_thin, result.dashed, result.dashed, result.refineDuels)
        } else {
            pluralStringResource(R.plurals.play_thin_refined, result.dashed, result.dashed)
        },
        modifier = Modifier.testTag(PlayTestTags.THIN),
    )
}

@Composable
private fun NoteBanner(text: String, modifier: Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            InfoIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(text = text, style = SMALL_LINE, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Nothing to preview when every duel was skipped: the cards would only pile into the first tier. */
@Composable
private fun PreviewRows(result: ResultState, narrowBands: Boolean) {
    if (!result.canApply) return
    Column(verticalArrangement = Arrangement.spacedBy(Rows.rowGap)) {
        result.tiers.forEach { PreviewRow(it, narrowBands) }
    }
}

/**
 * A tier as the board's compact rows draw it, without anything to drag. An
 * empty tier is as tall as one holding a line of cards, as on the board: a
 * low row would read as a tier that matters less.
 */
@Composable
private fun PreviewRow(tier: ResultTier, narrowBand: Boolean) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = Rows.occupiedHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.rowTint),
    ) {
        val name = listOfNotNull(tier.label, tier.caption).joinToString(" · ")
        Column(
            Modifier
                .width(if (narrowBand) NARROW_BAND else Rows.bandWidth)
                .fillMaxHeight()
                .background(colors.band)
                .then(if (narrowBand) Modifier.clearAndSetSemantics { contentDescription = name } else Modifier)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = tier.label,
                style = TierYourLifeType.current.tierBandLetter.merge(
                    TextStyle(fontSize = Rows.letterSize, lineHeight = Rows.letterLineHeight),
                ),
                color = colors.onBand,
                maxLines = 1,
            )
            if (!narrowBand) {
                tier.caption?.let { caption ->
                    Text(
                        text = caption,
                        style = TierYourLifeType.current.tierBandCaption.merge(
                            TextStyle(fontSize = Rows.captionSize, lineHeight = Rows.captionLineHeight),
                        ),
                        color = colors.onBand.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
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
                    .padding(Rows.padding),
                horizontalArrangement = Arrangement.spacedBy(Rows.gap),
                verticalArrangement = Arrangement.spacedBy(Rows.gap),
            ) {
                val outline = MaterialTheme.colorScheme.outline
                tier.cards.forEach { card ->
                    Box(if (card.dashed) Modifier.dashedBorder(1.5.dp, outline, 6.dp, dashLength = 4.dp, gapLength = 3.dp) else Modifier) {
                        ItemTile(card.item, Rows.tileWidth, Rows.tileHeight)
                    }
                }
            }
        }
    }
}

private val NARROW_BAND = 48.dp

/** Stacked full width on a phone, in the order of how likely each is; one row on anything wider. */
@Composable
private fun UprightButtons(result: ResultState, actions: ResultActions, oneRow: Boolean) {
    val padding = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    val refine = pluralStringResource(R.plurals.play_refine_up_to, result.refineDuels, result.refineDuels)
    if (oneRow) {
        Row(padding, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            CancelButton(actions.onCancel, Modifier)
            PlayAgainButton(actions.onPlayAgain, Modifier)
            if (result.canRefine) RefineButton(refine, actions.onRefine, Modifier)
            ApplyButton(result, actions.onApply, Modifier)
        }
    } else {
        Column(padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ApplyButton(result, actions.onApply, Modifier.fillMaxWidth())
            if (result.canRefine) RefineButton(refine, actions.onRefine, Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                PlayAgainButton(actions.onPlayAgain, Modifier)
                CancelButton(actions.onCancel, Modifier)
            }
        }
    }
}

@Composable
private fun ApplyButton(result: ResultState, onApply: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onApply,
        enabled = result.canApply,
        modifier = modifier
            .heightIn(min = 44.dp)
            .testTag(PlayTestTags.APPLY),
    ) {
        Text(stringResource(if (result.intoMine) R.string.play_put_in_mine else R.string.play_apply))
    }
}

@Composable
private fun RefineButton(label: String, onRefine: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onRefine,
        modifier = modifier
            .heightIn(min = 44.dp)
            .testTag(PlayTestTags.REFINE),
    ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
private fun PlayAgainButton(onPlayAgain: () -> Unit, modifier: Modifier) {
    TextButton(onClick = onPlayAgain, modifier = modifier.testTag(PlayTestTags.PLAY_AGAIN)) {
        Text(stringResource(R.string.play_again))
    }
}

@Composable
private fun CancelButton(onCancel: () -> Unit, modifier: Modifier) {
    TextButton(onClick = onCancel, modifier = modifier.testTag(PlayTestTags.CANCEL)) {
        Text(stringResource(R.string.play_cancel))
    }
}
