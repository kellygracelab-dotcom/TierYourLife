package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.artiuillab.tieryourlife.core.theme.ui.MoreIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlayArrowIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.play.PLAY_MIN_CARDS
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private const val TITLE_MAX_LENGTH = 60

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
    /** A reader's overflow: who wrote this, and how to stop seeing it. */
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
            if (titleEditable) {
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
            play?.let { PlayAction(it) }
            if (!readOnly) {
                IconButton(
                    onClick = onManualAdd,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = manualAddDescription }
                        .testTag(TierDetailTestTags.MANUAL_ADD_BUTTON),
                ) { AddPhotoIcon() }
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = moreDescription },
                ) { MoreIcon() }
            } else if (onReaderMoreClick != null) {
                IconButton(
                    onClick = onReaderMoreClick,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = moreDescription }
                        .testTag(TierDetailTestTags.MORE),
                ) { MoreIcon() }
            }
        }
    }
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
                if (newValue.text.length <= TITLE_MAX_LENGTH) fieldValue = newValue
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
