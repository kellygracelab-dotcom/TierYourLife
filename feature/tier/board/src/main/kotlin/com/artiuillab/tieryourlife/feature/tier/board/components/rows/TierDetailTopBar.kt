package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.tips.Tip
import com.artiuillab.tieryourlife.core.theme.tips.tipAnchor
import com.artiuillab.tieryourlife.core.theme.ui.AddPhotoIcon
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.core.theme.ui.CloudUploadIcon
import com.artiuillab.tieryourlife.core.theme.ui.FormatListNumberedIcon
import com.artiuillab.tieryourlife.core.theme.ui.GridViewIcon
import com.artiuillab.tieryourlife.core.theme.ui.LeaderboardIcon
import com.artiuillab.tieryourlife.core.theme.ui.MoreIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlayArrowIcon
import com.artiuillab.tieryourlife.core.theme.ui.PublishIcon
import com.artiuillab.tieryourlife.core.theme.ui.ViewCarouselIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.play.PLAY_MIN_CARDS
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import kotlin.math.roundToInt
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
    /** Tapped at the end of [subtitle]: someone else's list's rankings. */
    subtitleLink: SubtitleLink? = null,
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
    /** How the tiers are shown, first of the actions; it gives way on a narrow bar where Publish needs the room. */
    view: ViewButton? = null,
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
            // A short window's bar keeps a reader's line too, and Play its word: held sideways the bar is wide.
            val roomy = !compact || onReaderMoreClick != null
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
                ReadTitle(title, titleStyle, subline, subtitle.takeIf { roomy && subline == null }?.let { withLink(it, subtitleLink) })
            }
            trailing?.invoke(this)
            BarActions(publish, play, view, playWorded = roomy)
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

/** A line under a title that ends in a link: someone else's list's rankings. */
@Immutable
data class SubtitleLink(val text: String, val onClick: () -> Unit)

/** [text], and after it [link] in the primary colour, which can be tapped. */
@Composable
internal fun withLink(text: String, link: SubtitleLink?): AnnotatedString {
    val color = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        append(text)
        if (link != null) {
            append(SUBTITLE_SEPARATOR)
            withLink(LinkAnnotation.Clickable(SUBTITLE_LINK_TAG, TextLinkStyles(SpanStyle(color = color))) { link.onClick() }) {
                append(link.text)
            }
        }
    }
}

private const val SUBTITLE_SEPARATOR = " · "
private const val SUBTITLE_LINK_TAG = "subtitle_link"

/** A title only read, and the line under it: a tablet's [subline], or [subtitle]. */
@Composable
private fun RowScope.ReadTitle(title: String, style: TextStyle, subline: (@Composable () -> Unit)?, subtitle: AnnotatedString?) {
    Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
        Text(
            text = title,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        subline?.invoke()
        if (subtitle != null) {
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

/** The bar's way to show the tiers otherwise, as on the web: the current one's picture, the three in a menu. */
@Immutable
data class ViewButton(val mode: TierListDisplayMode, val onSelect: (TierListDisplayMode) -> Unit)

private val ViewModes = listOf(TierListDisplayMode.WRAP, TierListDisplayMode.HORIZONTAL_SCROLL, TierListDisplayMode.FLAT_RANKED)

@Composable
private fun ViewModeIcon(mode: TierListDisplayMode, color: Color) = when (mode) {
    TierListDisplayMode.WRAP -> GridViewIcon(24.dp, color)
    TierListDisplayMode.HORIZONTAL_SCROLL -> ViewCarouselIcon(24.dp, color)
    TierListDisplayMode.FLAT_RANKED -> FormatListNumberedIcon(24.dp, color)
}

fun viewModeLabel(mode: TierListDisplayMode): Int = when (mode) {
    TierListDisplayMode.WRAP -> R.string.tier_list_settings_mode_wrapped
    TierListDisplayMode.HORIZONTAL_SCROLL -> R.string.tier_list_settings_mode_strip
    TierListDisplayMode.FLAT_RANKED -> R.string.tier_list_settings_mode_ranked
}

@Composable
private fun ViewAction(view: ViewButton) {
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.tier_list_settings_display_section) + ": " + stringResource(viewModeLabel(view.mode))
    Box {
        IconButton(
            onClick = { open = true },
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = description }
                .testTag(TierDetailTestTags.VIEW_MODE),
        ) { ViewModeIcon(view.mode, MaterialTheme.colorScheme.onSurfaceVariant) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ViewModes.forEach { mode ->
                val chosen = mode == view.mode
                val tint = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                DropdownMenuItem(
                    text = { Text(stringResource(viewModeLabel(mode))) },
                    onClick = {
                        open = false
                        if (!chosen) view.onSelect(mode)
                    },
                    modifier = Modifier.testTag(TierDetailTestTags.viewMode(mode.name)),
                    leadingIcon = { ViewModeIcon(mode, tint) },
                    trailingIcon = if (chosen) {
                        { CheckIcon(20.dp, MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            NamesOnCardsItem(onDone = { open = false })
        }
    }
}

/** Under the three ways to show the tiers: the names on the pictures, on or off for every list on this device. */
@Composable
private fun NamesOnCardsItem(onDone: () -> Unit) {
    val memory = LocalBoardLayoutMemory.current
    val on = memory.namesOnCards
    DropdownMenuItem(
        text = {
            Column(Modifier.widthIn(max = NAMES_ITEM_MAX_WIDTH)) {
                Text(stringResource(R.string.board_names_on_cards))
                Text(
                    text = stringResource(R.string.board_names_on_cards_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = {
            onDone()
            memory.namesOnCards = !on
        },
        modifier = Modifier
            .semantics { toggleableState = ToggleableState(on) }
            .testTag(TierDetailTestTags.NAMES_ON_CARDS),
        leadingIcon = { Spacer(Modifier.size(24.dp)) },
        trailingIcon = { Box(Modifier.size(20.dp)) { if (on) CheckIcon(20.dp, MaterialTheme.colorScheme.primary) } },
    )
}

private val NAMES_ITEM_MAX_WIDTH = 220.dp

/** The bar's Play: how many cards there are decides whether it can start, a saved game puts a dot on it. */
@Immutable
data class PlayButton(val cards: Int, val saved: Boolean, val onClick: () -> Unit) {
    val enabled: Boolean get() = cards >= PLAY_MIN_CARDS
}

/**
 * Play, with its word and tonal, as Publish has its word: a lone triangle read
 * as a video. A short window's bar has room for the triangle only.
 */
@Composable
private fun PlayAction(play: PlayButton, worded: Boolean) {
    val description = stringResource(if (play.enabled) R.string.tier_detail_content_description_play else R.string.tier_detail_play_needs_cards)
    val color = if (play.enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
    }
    if (worded) {
        FilledTonalButton(
            onClick = play.onClick,
            enabled = play.enabled,
            contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .heightIn(min = 40.dp)
                .semantics { contentDescription = description }
                .tipAnchor(Tip.Play)
                .testTag(TierDetailTestTags.PLAY),
        ) {
            BadgedBox(badge = { if (play.saved && play.enabled) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
                PlayArrowIcon(18.dp, LocalContentColor.current)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.tier_detail_content_description_play),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        return
    }
    IconButton(
        onClick = play.onClick,
        enabled = play.enabled,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .tipAnchor(Tip.Play)
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
private fun BarActions(publish: PublishButton?, play: PlayButton?, view: ViewButton?, playWorded: Boolean) {
    if (view != null && publish?.narrow != true) ViewAction(view)
    publish?.let { PublishAction(it) }
    play?.let { PlayAction(it, worded = playWorded) }
}

@Composable
private fun PublishAction(publish: PublishButton) {
    // While it goes, the button says so in place and takes no second tap.
    val label = stringResource(if (publish.busy) sendingLabel(publish.kind) else publishLabel(publish.kind))
    val modifier = Modifier
        .padding(horizontal = 4.dp)
        .semantics { contentDescription = label }
        .testTag(TierDetailTestTags.PUBLISH)
    val content: @Composable RowScope.() -> Unit = {
        if (publish.busy) {
            CircularProgressIndicator(Modifier.size(16.dp).testTag(TierDetailTestTags.PUBLISH_PROGRESS), color = LocalContentColor.current, strokeWidth = 2.dp)
        } else {
            PublishingIcon(publish.kind, LocalContentColor.current)
        }
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

fun sendingLabel(kind: Publishing): Int = if (kind == Publishing.Update) R.string.board_updating else R.string.board_publishing

@Composable
fun PublishingIcon(kind: Publishing, tint: Color) = when (kind) {
    Publishing.Publish -> PublishIcon(18.dp, tint)
    Publishing.Update -> CloudUploadIcon(18.dp, tint)
    Publishing.ShareRanking -> LeaderboardIcon(18.dp, tint)
}

/** The publish button's room in the two-storey bar: back, Play's triangle, the overflow and their margins. */
val OWN_BAR_BESIDE_PUBLISH: Dp = 48.dp * 3 + 16.dp

/**
 * Play says its word only while Publish keeps its own on one line beside it;
 * on a narrow phone in a long language the triangle gives way, as the view button does.
 */
@Composable
private fun playKeepsItsWord(publish: PublishButton?, view: ViewButton?, width: Dp): Boolean {
    if (publish == null) return true
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    fun wide(text: String): Dp = with(density) { measurer.measure(text, style, maxLines = 1).size.width.toDp() }
    val publishText = wide(stringResource(if (publish.busy) sendingLabel(publish.kind) else publishLabel(publish.kind)))
    val publishWidth = 8.dp + (if (publish.narrow) 22.dp + 6.dp else 28.dp + 8.dp) + 18.dp + publishText
    val playWidth = 8.dp + 28.dp + 18.dp + 6.dp + wide(stringResource(R.string.tier_detail_content_description_play))
    val icons = 8.dp + 48.dp * 2 + (if (publish.narrow) 0.dp else 48.dp) + (if (view != null && !publish.narrow) 48.dp else 0.dp)
    return icons + publishWidth + playWidth <= width
}

/**
 * A board on a phone held upright: a bar in two storeys. Above, back, Publish
 * and Play with their words and the overflow; below, the whole name and what
 * the board holds. One's own name is renamed by a tap as before; someone
 * else's ([onRenameList] null) is only read, with its section in the line
 * under it. Scrolling the rows folds the lower storey away and brings a short
 * title into the upper one, the standard exit-until-collapsed behaviour;
 * [state] is that behaviour's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnBoardTopBar(
    title: String,
    counts: String,
    state: TopAppBarState,
    onBack: () -> Unit,
    onManualAdd: (() -> Unit)?,
    onMoreClick: () -> Unit,
    onRenameList: ((String) -> Unit)?,
    play: PlayButton?,
    publish: PublishButton?,
    view: ViewButton?,
    moreTag: String? = null,
    /** Tapped at the end of [counts]: someone else's list's rankings. */
    countsLink: SubtitleLink? = null,
) {
    val backDescription = stringResource(ThemeR.string.tier_detail_content_description_back)
    val moreDescription = stringResource(ThemeR.string.tier_detail_content_description_more)
    val folded = state.collapsedFraction
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag(TierDetailTestTags.OWN_BAR),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val barWidth = maxWidth
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = backDescription },
            ) { BackIcon() }
            // The short title only once the long one is mostly gone; until then it would be said twice.
            Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                if (folded > FOLDED_TITLE_FROM) {
                    Text(
                        text = title,
                        modifier = Modifier.graphicsLayer { alpha = (folded - FOLDED_TITLE_FROM) / (1f - FOLDED_TITLE_FROM) },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            BarActions(publish, play, view, playWorded = playKeepsItsWord(publish, view, barWidth))
            if (onManualAdd != null && publish?.narrow != true) {
                ManualAddAction(onManualAdd, stringResource(R.string.tier_detail_content_description_manual_add))
            }
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = moreDescription }
                    .then(if (moreTag != null) Modifier.testTag(moreTag) else Modifier),
            ) { MoreIcon() }
        }
        }
        FoldingStorey(state) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                val titleStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp)
                if (onRenameList != null) {
                    EditableListTitle(
                        title = title,
                        onRename = onRenameList,
                        style = titleStyle,
                        modifier = Modifier.fillMaxWidth(),
                        wraps = true,
                    )
                } else {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth().testTag(TierDetailTestTags.HEADER_TITLE),
                        style = titleStyle,
                        maxLines = WRAPPED_TITLE_LINES,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = withLink(counts, countsLink),
                    modifier = Modifier.padding(top = 2.dp).testTag(TierDetailTestTags.OWN_BAR_COUNTS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // The link takes a second line rather than being cut.
                    maxLines = if (countsLink != null) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private const val FOLDED_TITLE_FROM = 0.5f

/** The lower storey, as tall as it is less what scrolling took, sliding up under the upper one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoldingStorey(state: TopAppBarState, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.clipToBounds()) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val full = placeable.height
        if (state.heightOffsetLimit != -full.toFloat()) state.heightOffsetLimit = -full.toFloat()
        val shown = (full + state.heightOffset).roundToInt().coerceIn(0, full)
        layout(placeable.width, shown) { placeable.place(0, shown - full) }
    }
}

/** "12 cards · 5 placed", or "1 card · nothing placed yet". */
@Composable
fun boardCounts(list: TierList): String {
    val cards = list.tiers.sumOf { it.items.size }
    val placed = list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
    val all = pluralStringResource(R.plurals.board_cards_count, cards, cards)
    val done = if (placed == 0) stringResource(R.string.board_nothing_placed) else pluralStringResource(R.plurals.board_cards_placed, placed, placed)
    return stringResource(R.string.board_header_counts, all, done)
}

@Composable
private fun EditableListTitle(
    title: String,
    onRename: (String) -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier,
    /** The whole name on as many lines as it takes, as the two-storey bar shows it. */
    wraps: Boolean = false,
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
            singleLine = !wraps,
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
            maxLines = if (wraps) WRAPPED_TITLE_LINES else 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val WRAPPED_TITLE_LINES = 3
