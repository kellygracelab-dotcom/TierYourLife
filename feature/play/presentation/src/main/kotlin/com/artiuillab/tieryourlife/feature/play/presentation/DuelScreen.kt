package com.artiuillab.tieryourlife.feature.play.presentation

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.core.theme.ui.CardLabel
import com.artiuillab.tieryourlife.core.theme.ui.ClearIcon
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The duels themselves: two cards, tap the better one. Nothing else on screen asks for a decision. */
@Composable
internal fun DuelScreen(
    duel: DuelState,
    onPick: (first: Boolean) -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onQuit: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(PlayTestTags.DUEL),
        color = MaterialTheme.colorScheme.surface,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val short = WindowShape.isShort(maxHeight)
            val metrics = DuelMetrics.of(maxWidth, maxHeight)
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                PlayHeader(
                    title = stringResource(duel.kind.nameRes),
                    line = duelLine(duel),
                    short = short,
                    onQuit = onQuit,
                    trailing = {
                        if (duel.rounds > 0 && !duel.refining) {
                            Text(
                                text = stringResource(R.string.play_round_of, duel.round, duel.rounds),
                                modifier = Modifier.padding(horizontal = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val undo = stringResource(R.string.play_undo_pick)
                        IconButton(
                            onClick = onUndo,
                            enabled = duel.canUndo,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = undo }
                                .testTag(PlayTestTags.UNDO),
                        ) {
                            val color = MaterialTheme.colorScheme.onSurfaceVariant
                            UndoIcon(24.dp, if (duel.canUndo) color else color.copy(alpha = 0.38f))
                        }
                    },
                )
                val progress by animateFloatAsState(duel.progress, tween(PROGRESS_MILLIS), label = "duel progress")
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DuelStage(duel = duel, metrics = metrics, onPick = onPick)
                }
                DuelFooter(duel = duel, onSkip = onSkip)
            }
        }
    }
}

private const val PROGRESS_MILLIS = 200

@Composable
private fun duelLine(duel: DuelState): String = if (duel.refining) {
    stringResource(R.string.play_refine_of, duel.number, duel.total)
} else {
    stringResource(R.string.play_duel_of, duel.number, duel.total)
}

/** The bar over the duels and the result: Quit, the game's name, and a line under it. */
@Composable
internal fun PlayHeader(
    title: String,
    line: String?,
    short: Boolean,
    onQuit: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    val quit = stringResource(R.string.play_quit)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(if (short) 48.dp else 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onQuit,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = quit }
                .testTag(PlayTestTags.QUIT),
        ) { ClearIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = title,
                style = if (short) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (line != null) {
                Text(
                    text = line,
                    // Read out as it changes, so a pick is heard to have counted.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = if (short) MaterialTheme.typography.labelMedium else HEADER_LINE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}

private val HEADER_LINE = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)

/** How big the cards are drawn, by the window: the design's four sizes. */
internal data class DuelMetrics(val width: Dp, val height: Dp, val gap: Dp, val name: TextStyle, val nameLines: Int) {
    companion object {
        fun of(width: Dp, height: Dp): DuelMetrics = when {
            WindowShape.isShort(height) -> {
                val cardHeight = (height - SHORT_CHROME).coerceAtLeast(SHORT_CARD_FLOOR)
                DuelMetrics(cardHeight * CARD_RATIO, cardHeight, SHORT_GAP, SHORT_NAME, 2)
            }
            width >= WindowShape.WideFrom -> WIDE
            width >= WindowShape.MediumFrom -> MEDIUM
            else -> {
                val cardWidth = (width - PHONE_MARGINS) / 2
                DuelMetrics(cardWidth, cardWidth / CARD_RATIO, PHONE_GAP, PHONE_NAME, PHONE_NAME_LINES)
            }
        }
    }
}

private const val CARD_RATIO = 0.711f
private const val PHONE_NAME_LINES = 3

/** The header, the progress and Skip: what a short window leaves the cards is its height less this. */
private val SHORT_CHROME = 150.dp
private val SHORT_CARD_FLOOR = 120.dp
private val SHORT_GAP = 24.dp

/** Two margins of 16 and the 12 between the cards. */
private val PHONE_MARGINS = 44.dp
private val PHONE_GAP = 12.dp

private fun nameStyle(size: Int, line: Int) = TextStyle(fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight.Medium)

private val SHORT_NAME = nameStyle(14, 20)
private val PHONE_NAME = nameStyle(15, 20)
private val WIDE = DuelMetrics(300.dp, 422.dp, 48.dp, nameStyle(20, 26), 2)
private val MEDIUM = DuelMetrics(240.dp, 338.dp, 32.dp, nameStyle(18, 24), 2)

@Composable
private fun DuelStage(duel: DuelState, metrics: DuelMetrics, onPick: (first: Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val still = reducedMotion()
    // Which card was just chosen, held for the moment its ring shows; a new duel clears it.
    var chosen by remember(duel) { mutableStateOf<Boolean?>(null) }
    val pick: (Boolean) -> Unit = { first ->
        if (chosen == null) {
            chosen = first
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            scope.launch {
                delay(if (still) 0L else CHOSEN_MILLIS.toLong())
                onPick(first)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(metrics.gap), verticalAlignment = Alignment.Top) {
        DuelSlot(duel.first, metrics, chosen = chosen, still = still, tag = PlayTestTags.FIRST_CARD) { pick(true) }
        DuelSlot(duel.second, metrics, chosen = chosen?.let { !it }, still = still, tag = PlayTestTags.SECOND_CARD) { pick(false) }
    }
}

private const val CHOSEN_MILLIS = 160
private const val ARRIVE_MILLIS = 200
private const val LEAVE_MILLIS = 90

/** One side of the duel. A card that stays (the hill's champion) does not move; a new one rises in. */
@Composable
private fun DuelSlot(card: TierItem, metrics: DuelMetrics, chosen: Boolean?, still: Boolean, tag: String, onPick: () -> Unit) {
    val rise = with(LocalDensity.current) { 24.dp.roundToPx() }
    AnimatedContent(
        targetState = card,
        contentKey = { it.id },
        transitionSpec = {
            if (still) {
                fadeIn(tween(LEAVE_MILLIS)) togetherWith fadeOut(tween(LEAVE_MILLIS))
            } else {
                (slideInVertically(tween(ARRIVE_MILLIS)) { rise } + fadeIn(tween(ARRIVE_MILLIS))) togetherWith fadeOut(tween(LEAVE_MILLIS))
            }
        },
        label = "duel card",
    ) { shown ->
        DuelCard(card = shown, metrics = metrics, chosen = chosen, still = still, tag = tag, onPick = onPick)
    }
}

@Composable
private fun DuelCard(card: TierItem, metrics: DuelMetrics, chosen: Boolean?, still: Boolean, tag: String, onPick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val description = stringResource(R.string.play_pick, card.title)
    val scale by animateFloatAsState(if (chosen == true && !still) CHOSEN_SCALE else 1f, tween(CHOSEN_MILLIS), label = "chosen scale")
    val fade by animateFloatAsState(if (chosen == false) PASSED_ALPHA else 1f, tween(CHOSEN_MILLIS), label = "passed fade")
    Column(
        modifier = Modifier
            .width(metrics.width)
            .alpha(fade)
            .clickable(onClick = onPick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                testTag = tag
                onClick {
                    onPick()
                    true
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(metrics.width, metrics.height)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(TierYourLifeMedia.current.tilePlaceholder)
                .then(if (chosen == true) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (card.imageUrl != null) {
                AsyncImage(
                    model = card.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CardLabel(
                        text = card.title,
                        style = TextOnlyName,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 5,
                        smallest = 14.sp,
                    )
                }
            }
        }
        // A card without a picture already wears its name.
        if (card.imageUrl != null) {
            Text(
                text = card.title,
                modifier = Modifier.padding(top = 12.dp),
                style = metrics.name,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = metrics.nameLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val CHOSEN_SCALE = 1.04f
private const val PASSED_ALPHA = 0.4f
private val TextOnlyName = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)

@Composable
private fun DuelFooter(duel: DuelState, onSkip: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier
                .height(48.dp)
                .testTag(PlayTestTags.SKIP),
        ) { Text(stringResource(R.string.play_skip)) }
        // Kept in place once it stops showing, so the cards do not jump on the fourth duel.
        Text(
            text = stringResource(R.string.play_skip_hint),
            modifier = Modifier
                .padding(top = 8.dp)
                .alpha(if (duel.explainsSkip) 1f else 0f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}

/** The system's "remove animations": then only a short cross-fade, no rising, no growing. */
@Composable
private fun reducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
