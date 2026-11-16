package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.ui.AddPhotoIcon
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.core.theme.ui.CloudUploadIcon
import com.artiuillab.tieryourlife.core.theme.ui.LeaderboardIcon
import com.artiuillab.tieryourlife.core.theme.ui.MoreIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlayArrowIcon
import com.artiuillab.tieryourlife.core.theme.ui.PublishIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.play.PLAY_MIN_CARDS
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private const val TITLE_MAX_LENGTH = 80

@Composable
fun TierScreenTopBar(
    title: String,
    onBack: () -> Unit,
    onManualAdd: () -> Unit,
    onMoreClick: () -> Unit = {},
    onRenameList: (String) -> Unit = {},
    titleEditable: Boolean = false,
    /** Someone else's list: nothing here is editable, so nothing offers to edit. */
    readOnly: Boolean = false,
    /** The community's menu also applies while arranging one's own ranking. */
    onReaderMoreClick: (() -> Unit)? = null,
    subtitle: String? = null,
    /** A short window's one bar: lower, a smaller title, and no second line. */
    compact: Boolean = false,
    /** What the screen around the board puts in the bar, between the title and the overflow: a reader's switch and save. */
    trailing: (@Composable RowScope.() -> Unit)? = null,
    /** False where the bar scrolls away: the inset then stays with the screen, or the content would slide under the clock. */
    insetStatusBar: Boolean = true,
    /** True when the pool stands beside the rows: the board then spans the window, and its bar with it. */
    spanWindow: Boolean = false,
    /** A height other than the usual 56 (48 when compact): a tablet's taller bar. */
    barHeight: Dp? = null,
    /** A line under the title in place of [subtitle]: a tablet's author and section, which can be tapped. */
    subline: (@Composable () -> Unit)? = null,
    /** Play, first of the bar's actions; null leaves it out. */
    play: PlayButton? = null,
    /** Publish, Update or Share ranking, before Play; null leaves it out. */
    publish: PublishButton? = null,
) {
    val backDescription = stringResource(ThemeR.string.tier_detail_content_description_back)
    val manualAddDescription = stringResource(R.string.tier_detail_content_description_manual_add)
    val moreDescription = stringResource(ThemeR.string.tier_detail_content_description_more)

    // The bar spans the window; what it holds keeps the board's measure, so the
    // back arrow and the overflow stay either side of the board, not the screen.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (insetStatusBar) Modifier.statusBarsPadding() else Modifier)
            .background(MaterialTheme.colorScheme.surface)
            .height(barHeight ?: if (compact) 48.dp else 56.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = if (spanWindow) Modifier.fillMaxWidth() else Modifier.atMost(ContentWidth.Board),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = backDescription },
            ) { BackIcon() }
            val titleStyle = when {
                compact -> MaterialTheme.typography.titleMedium
                subline != null -> MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp)
                else -> MaterialTheme.typography.titleLarge
            }
            if (titleEditable && onReaderMoreClick == null) {
                EditableListTitle(
                    title = title,
                    onRename = onRenameList,
                    style = titleStyle,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                )
            } else {
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(
                        text = title,
                        style = titleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    subline?.invoke()
                    if (subtitle != null && !compact && subline == null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            trailing?.invoke(this)
            BarActions(publish, play)
            if (onReaderMoreClick != null) {
                // Moving cards in Mine does not make the reader the list's editor.
                IconButton(
                    onClick = onReaderMoreClick,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = moreDescription }
                        .testTag(TierDetailTestTags.MORE),
                ) { MoreIcon() }
            } else if (!readOnly) {
                if (publish?.narrow != true) ManualAddAction(onManualAdd, manualAddDescription)
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = moreDescription },
                ) { MoreIcon() }
            }
        }
    }
}

@Composable
private fun ManualAddAction(onManualAdd: () -> Unit, description: String) {
    IconButton(
        onClick = onManualAdd,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .testTag(TierDetailTestTags.MANUAL_ADD_BUTTON),
    ) { AddPhotoIcon() }
}

/** The bar's Play: how many cards there are decides whether it can start, a saved game puts a dot on it. */
@Immutable
data class PlayButton(val cards: Int, val saved: Boolean, val onClick: () -> Unit) {
    val enabled: Boolean get() = cards >= PLAY_MIN_CARDS
}

@Composable
private fun PlayAction(play: PlayButton) {
    val description = stringResource(if (play.enabled) R.string.tier_detail_content_description_play else R.string.tier_detail_play_needs_cards)
    val color = if (play.enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
    }
    IconButton(
        onClick = play.onClick,
        enabled = play.enabled,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .testTag(TierDetailTestTags.PLAY),
    ) {
        BadgedBox(badge = { if (play.saved && play.enabled) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
            PlayArrowIcon(24.dp, color)
        }
    }
}

private const val DISABLED_ALPHA = 0.38f

/** What the bar offers to send out: a board of one's own, a published one that moved on, a copy as one's ranking. */
enum class Publishing { Publish, Update, ShareRanking }

/**
 * The bar's publish button, always with its word: an arrow alone reads as upload
 * or share. [primary] once every card is placed: filled; tonal before. On a
 * [narrow] bar it is tighter, and the camera gives it its room by moving to the
 * list's page behind the overflow; the title shortens with an ellipsis.
 */
@Immutable
data class PublishButton(
    val kind: Publishing,
    val primary: Boolean,
    val narrow: Boolean,
    /** At most this wide, so the title keeps a few letters; a longer word wraps to a second line. */
    val room: Dp = Dp.Unspecified,
    val busy: Boolean,
    val onClick: () -> Unit,
)

/** Nothing to send from an empty board, nor from a published one that has not moved on. */
fun publishingOf(list: TierList, publishedIsBehind: Boolean): Publishing? = when {
    list.tiers.none { it.items.isNotEmpty() } -> null
    list.isPublic -> Publishing.Update.takeIf { publishedIsBehind }
    list.takenFrom != null -> Publishing.ShareRanking
    else -> Publishing.Publish
}

/** Every card is out of the pool: publishing is what is left to do. */
fun everyCardPlaced(list: TierList): Boolean =
    list.tiers.any { it.items.isNotEmpty() } && list.tiers.filter { it.isPool }.all { it.items.isEmpty() }

/** The bar's actions after the title: publishing, then Play. */
@Composable
private fun BarActions(publish: PublishButton?, play: PlayButton?) {
    publish?.let { PublishAction(it) }
    play?.let { PlayAction(it) }
}

@Composable
private fun PublishAction(publish: PublishButton) {
    val label = stringResource(publishLabel(publish.kind))
    val modifier = Modifier
        .padding(horizontal = 4.dp)
        .semantics { contentDescription = label }
        .testTag(TierDetailTestTags.PUBLISH)
    val content: @Composable RowScope.() -> Unit = {
        PublishingIcon(publish.kind, LocalContentColor.current)
        Spacer(Modifier.width(if (publish.narrow) 6.dp else 8.dp))
        Text(
            label,
            style = if (publish.narrow) MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp) else MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
    val padding = if (publish.narrow) PaddingValues(start = 10.dp, top = 4.dp, end = 12.dp, bottom = 4.dp) else PaddingValues(start = 12.dp, end = 16.dp)
    val sized = modifier.heightIn(min = 36.dp).widthIn(max = publish.room)
    if (publish.primary) {
        Button(onClick = publish.onClick, enabled = !publish.busy, modifier = sized, contentPadding = padding, content = content)
    } else {
        FilledTonalButton(onClick = publish.onClick, enabled = !publish.busy, modifier = sized, contentPadding = padding, content = content)
    }
}

/** The one wording for publishing, in the bar and in the board's settings alike. */
fun publishLabel(kind: Publishing): Int = when (kind) {
    Publishing.Publish -> R.string.board_publish
    Publishing.Update -> R.string.board_publish_update
    Publishing.ShareRanking -> R.string.board_publish_ranking
}

@Composable
fun PublishingIcon(kind: Publishing, tint: Color) = when (kind) {
    Publishing.Publish -> PublishIcon(18.dp, tint)
    Publishing.Update -> CloudUploadIcon(18.dp, tint)
    Publishing.ShareRanking -> LeaderboardIcon(18.dp, tint)
}

@Composable
private fun EditableListTitle(
    title: String,
    onRename: (String) -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember { mutableStateOf(false) }

    if (isEditing) {
        var fieldValue by remember {
            mutableStateOf(TextFieldValue(text = title, selection = TextRange(title.length)))
        }
        var hasFocused by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        fun commit() {
            if (!isEditing) return
            val trimmed = fieldValue.text.trim()
            if (trimmed.isNotEmpty()) onRename(trimmed)
            isEditing = false
        }

        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val shrinking = newValue.text.length < fieldValue.text.length
                if (shrinking || newValue.text.length <= TITLE_MAX_LENGTH) fieldValue = newValue
            },
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) hasFocused = true else if (hasFocused) commit()
                }
                .testTag(TierDetailTestTags.HEADER_TITLE),
            textStyle = style.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
        )

        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    } else {
        val editDescription = stringResource(R.string.tier_detail_content_description_edit_title)
        Text(
            text = title,
            modifier = modifier
                .clickable { isEditing = true }
                .semantics { contentDescription = editDescription }
                .testTag(TierDetailTestTags.HEADER_TITLE),
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
