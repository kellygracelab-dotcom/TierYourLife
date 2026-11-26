package com.artiuillab.tieryourlife.feature.tier.board.components.sheets

import android.content.res.Resources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.layout.BarredScreen
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.layout.topBarHeight
import com.artiuillab.tieryourlife.core.theme.layout.topBarTitleStyle
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.core.theme.ui.CategoryIcon
import com.artiuillab.tieryourlife.core.theme.ui.ChevronRightIcon
import com.artiuillab.tieryourlife.core.theme.ui.CloudOffIcon
import com.artiuillab.tieryourlife.core.theme.ui.CoverIcon
import com.artiuillab.tieryourlife.core.theme.ui.DeleteIcon
import com.artiuillab.tieryourlife.core.theme.ui.FormatListNumberedIcon
import com.artiuillab.tieryourlife.core.theme.ui.GridViewIcon
import com.artiuillab.tieryourlife.core.theme.ui.LeaderboardIcon
import com.artiuillab.tieryourlife.core.theme.ui.OpenInNewIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.core.theme.ui.RestoreIcon
import com.artiuillab.tieryourlife.core.theme.ui.ReturnToPoolDialog
import com.artiuillab.tieryourlife.core.theme.ui.SectionLabel
import com.artiuillab.tieryourlife.core.theme.ui.ShareIcon
import com.artiuillab.tieryourlife.core.theme.ui.TuneIcon
import com.artiuillab.tieryourlife.core.theme.ui.ViewCarouselIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.LocalSections
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.Publishing
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.PublishingIcon
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.publishLabel
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.publishingOf
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private val DisplayModeSelectedTintLight = Color(0xFFEDEBFA)
private val DisplayModeSelectedTintDark = Color(0xFF2E2F45)

@Composable
fun TierListSettingsScreenContent(
    list: TierList,
    onBack: () -> Unit,
    onAddTier: (label: String, caption: String?, colorLight: String, colorDark: String) -> Unit,
    onSetDisplayMode: (displayMode: TierListDisplayMode) -> Unit,
    signedIn: Boolean = false,
    publishing: Boolean = false,
    publishError: PublishError? = null,
    publicPending: Boolean? = null,
    categoryWanted: Boolean = false,
    onSetPublic: (Boolean) -> Unit = {},
    publishedIsBehind: Boolean = false,
    awaitingApproval: Boolean = false,
    onUpdatePublished: () -> Unit = {},
    /** The bar's publish, so both places say and do the same; null falls back to [onSetPublic] and [onUpdatePublished]. */
    onPublish: ((Publishing) -> Unit)? = null,
    onSetCategory: (String) -> Unit = {},
    onCategoryNotChosen: () -> Unit = {},
    /** Opens the proposal form, filing lists under the board's own section meanwhile; null where there is none. */
    onProposeSection: ((String?) -> Unit)? = null,
    onChooseCover: () -> Unit = {},
    onAdjustCover: () -> Unit = {},
    onChooseTopic: () -> Unit = {},
    onAddCards: () -> Unit = {},
    onShare: () -> Unit = {},
    /** The published list's link to send, as the bar's share does; null while it is not out. */
    onShareLink: (() -> Unit)? = null,
    /** The published list's own page, where readers' rankings and its link are; null while it is not out. */
    onSeeInCommunity: (() -> Unit)? = null,
    /** The Community list a copy was made from; there whether or not the link under the title was hidden. */
    onOpenOriginal: (() -> Unit)? = null,
    onReturnAllToPool: (() -> Unit)? = null,
    /** Into the trash after asking; null where there is no deleting from here. */
    onDeleteList: (() -> Unit)? = null,
) {
    var deleteVisible by rememberSaveable { mutableStateOf(false) }
    var tierEditorVisible by rememberSaveable { mutableStateOf(false) }
    var returnToPoolVisible by rememberSaveable { mutableStateOf(false) }
    var categorySheetVisible by rememberSaveable { mutableStateOf(false) }
    val rankedTierCount = list.tiers.count { !it.isPool }
    val published = publicPending ?: list.isPublic

    Column(Modifier.fillMaxSize().testTag(TierDetailTestTags.LIST_SETTINGS_SCREEN)) {
        BarredScreen(
            bar = {
                ListSettingsTopBar(onBack = onBack)
            },
        ) {
            Column(Modifier.fillMaxSize()) {
                // One scrolling column, kept clear of the navigation bar at the
                // bottom and of the system bars at the sides when sideways, so
                // the last row is never under them.
                CenteredContent(
                    ContentWidth.Reading,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)))
                        .padding(bottom = 16.dp),
                ) {
                    // Three groups: what others see of the list, the list itself, and
                    // last, apart and in the warning colour, what cannot be taken back lightly.
                    PublicationGroup(
                        list = list,
                        published = published,
                        signedIn = signedIn,
                        publishing = publishing,
                        publishError = publishError,
                        publishedIsBehind = publishedIsBehind,
                        awaitingApproval = awaitingApproval,
                        publicPending = publicPending,
                        onPublish = onPublish,
                        onSetPublic = onSetPublic,
                        onUpdatePublished = onUpdatePublished,
                        onAddCards = onAddCards,
                        onSeeInCommunity = onSeeInCommunity,
                        onShareLink = onShareLink,
                        onOpenOriginal = onOpenOriginal,
                        onChooseCategory = { categorySheetVisible = true },
                        onChooseTopic = onChooseTopic,
                        onChooseCover = onChooseCover,
                        onAdjustCover = onAdjustCover,
                        onShare = onShare,
                    )

                    GroupLabel(stringResource(R.string.list_settings_group_list))
                    NewTierRow(
                        tierCount = rankedTierCount,
                        hasRoom = list.hasRoomForATier,
                        onClick = { tierEditorVisible = true },
                    )
                    DisplayModeSection(selected = list.displayMode, onSelect = onSetDisplayMode)

                    CarefulGroup(
                        onReturnAllToPool = onReturnAllToPool?.let { { returnToPoolVisible = true } },
                        hasPlacedCards = list.tiers.any { !it.isPool && it.items.isNotEmpty() },
                        // Down while it is going down already: nothing to offer.
                        onTakeDown = { onSetPublic(false) }.takeIf { published && publicPending != false },
                        takeDownEnabled = !publishing,
                        onDelete = onDeleteList?.let { { deleteVisible = true } },
                    )
                }
            }
        }
    }

    if (returnToPoolVisible) {
        ReturnToPoolDialog(
            onDismiss = { returnToPoolVisible = false },
            onConfirm = {
                returnToPoolVisible = false
                onReturnAllToPool?.invoke()
            },
        )
    }

    if (deleteVisible && onDeleteList != null) {
        DeleteListDialog(
            title = list.title,
            published = published,
            onDismiss = { deleteVisible = false },
            onConfirm = {
                deleteVisible = false
                onDeleteList()
            },
        )
    }

    if (categorySheetVisible || categoryWanted) {
        CategorySheet(
            selected = list.category,
            onDismiss = {
                categorySheetVisible = false
                onCategoryNotChosen()
            },
            onSelect = {
                onSetCategory(it)
                categorySheetVisible = false
            },
            withWaiting = true,
            onPropose = onProposeSection?.let { propose ->
                {
                    categorySheetVisible = false
                    onCategoryNotChosen()
                    propose(list.category)
                }
            },
        )
    }

    if (tierEditorVisible) {
        TierEditorSheet(
            onDismiss = { tierEditorVisible = false },
            onSave = { label, caption, colorLight, colorDark ->
                onAddTier(label, caption, colorLight, colorDark)
                tierEditorVisible = false
            },
        )
    }
}

@Composable
private fun ListSettingsTopBar(onBack: () -> Unit) {
    val backDescription = stringResource(ThemeR.string.tier_detail_content_description_back)
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .atMost(ContentWidth.Reading)
                .height(topBarHeight())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = backDescription },
            ) { BackIcon() }
            Text(
                text = stringResource(R.string.tier_list_settings_title),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                style = topBarTitleStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DisplayModeSection(
    selected: TierListDisplayMode,
    onSelect: (TierListDisplayMode) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.tier_list_settings_display_section),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.tier_list_settings_display_section_caption),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            style = TierYourLifeType.current.supportingLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.selectableGroup()) {
            DisplayModeRow(
                selected = selected == TierListDisplayMode.WRAP,
                icon = { size, color -> GridViewIcon(size, color) },
                title = stringResource(R.string.tier_list_settings_mode_wrapped),
                subtitle = stringResource(R.string.tier_list_settings_mode_wrapped_sub),
                onClick = { onSelect(TierListDisplayMode.WRAP) },
                testTag = TierDetailTestTags.LIST_SETTINGS_MODE_WRAP,
            )
            DisplayModeRow(
                selected = selected == TierListDisplayMode.HORIZONTAL_SCROLL,
                icon = { size, color -> ViewCarouselIcon(size, color) },
                title = stringResource(R.string.tier_list_settings_mode_strip),
                subtitle = stringResource(R.string.tier_list_settings_mode_strip_sub),
                onClick = { onSelect(TierListDisplayMode.HORIZONTAL_SCROLL) },
                testTag = TierDetailTestTags.LIST_SETTINGS_MODE_STRIP,
            )
            DisplayModeRow(
                selected = selected == TierListDisplayMode.FLAT_RANKED,
                icon = { size, color -> FormatListNumberedIcon(size, color) },
                title = stringResource(R.string.tier_list_settings_mode_ranked),
                subtitle = stringResource(R.string.tier_list_settings_mode_ranked_sub),
                onClick = { onSelect(TierListDisplayMode.FLAT_RANKED) },
                testTag = TierDetailTestTags.LIST_SETTINGS_MODE_RANKED,
            )
        }
        NamesOnCardsRow()
    }
}

/** Under the three ways to show the tiers: the names on the pictures, on or off for every list on this device. */
@Composable
private fun NamesOnCardsRow() {
    val memory = LocalBoardLayoutMemory.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = memory.namesOnCards, role = Role.Switch, onValueChange = { memory.namesOnCards = it })
            .testTag(TierDetailTestTags.NAMES_ON_CARDS)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(R.string.board_names_on_cards), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = stringResource(R.string.board_names_on_cards_sub),
                style = TierYourLifeType.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = memory.namesOnCards, onCheckedChange = null)
    }
}

@Composable
private fun DisplayModeRow(
    selected: Boolean,
    icon: @Composable (Dp, Color) -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val isDark = TierYourLifeMedia.current.isDark
    val background = if (selected) {
        if (isDark) DisplayModeSelectedTintDark else DisplayModeSelectedTintLight
    } else {
        Color.Transparent
    }
    val accent = MaterialTheme.colorScheme.primary
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioDot(
            selected = selected,
            color = if (selected) accent else MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp),
        )
        icon(24.dp, if (selected) accent else neutral)
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = TierYourLifeType.current.supportingLabel, color = neutral)
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(20.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawCircle(color, radius = (20.dp.toPx() - strokeWidth) / 2f, style = Stroke(strokeWidth))
        if (selected) {
            drawCircle(color, radius = 5.dp.toPx())
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else DISABLED_ALPHA),
            )
            Text(
                text = subtitle,
                style = TierYourLifeType.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (enabled) ChevronRightIcon(20.dp, MaterialTheme.colorScheme.outline)
    }
}

private const val DISABLED_ALPHA = 0.38f

@Composable
private fun CategoryRow(category: String?, onClick: () -> Unit) {
    SettingsActionRow(
        icon = { CategoryIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.list_settings_category),
        subtitle = category?.let { id ->
            // Their own waiting proposal: the list waits elsewhere and says where it is going.
            if (LocalSections.current.directory.waiting.any { it.id == id }) {
                stringResource(R.string.list_settings_moves_to, sectionLabel(id))
            } else {
                sectionLabel(id)
            }
        } ?: stringResource(R.string.list_settings_category_none),
        onClick = onClick,
        testTag = TierDetailTestTags.CATEGORY_ROW,
    )
}

/** The word inside the section; a name still waiting for a moderator says so beside it. */
@Composable
private fun TopicRow(topicName: String?, newTopicName: String?, onClick: () -> Unit) {
    SettingsActionRow(
        icon = { CategoryIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.list_settings_topic),
        subtitle = when {
            newTopicName != null -> stringResource(R.string.list_settings_topic_waiting, newTopicName)
            topicName != null -> topicName
            else -> stringResource(R.string.list_settings_topic_none)
        },
        onClick = onClick,
        testTag = TierDetailTestTags.TOPIC_ROW,
    )
}

@Composable
private fun CoverRow(chosen: Boolean, onClick: () -> Unit) {
    SettingsActionRow(
        icon = { CoverIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.list_settings_cover),
        subtitle = stringResource(
            if (chosen) R.string.list_settings_cover_set else R.string.list_settings_cover_auto,
        ),
        onClick = onClick,
        testTag = TierDetailTestTags.COVER_ROW,
    )
}

@Composable
private fun CoverRows(list: TierList, onChooseCover: () -> Unit, onAdjustCover: () -> Unit) {
    CoverRow(chosen = list.coverImageUrl != null || list.coverImageUrls.isNotEmpty(), onClick = onChooseCover)
    // Only a single picture has a part to choose; a collage and the cards' art have none.
    if (list.coverImageUrl != null) AdjustCoverRow(onClick = onAdjustCover)
}

@Composable
private fun AdjustCoverRow(onClick: () -> Unit) {
    SettingsActionRow(
        icon = { TuneIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.list_settings_cover_adjust),
        subtitle = stringResource(R.string.list_settings_cover_adjust_sub),
        onClick = onClick,
        testTag = TierDetailTestTags.ADJUST_COVER_ROW,
    )
}

@Composable
private fun ShareRow(onClick: () -> Unit) {
    SettingsActionRow(
        icon = { ShareIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.list_settings_share),
        subtitle = stringResource(R.string.list_settings_share_sub),
        onClick = onClick,
        testTag = TierDetailTestTags.SHARE_ROW,
    )
}

@Composable
private fun OpenOriginalRow(onClick: () -> Unit) {
    SettingsActionRow(
        icon = { LeaderboardIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.list_settings_original),
        subtitle = stringResource(R.string.list_settings_original_sub),
        onClick = onClick,
        testTag = TierDetailTestTags.OPEN_ORIGINAL_ROW,
    )
}

@Composable
private fun GroupLabel(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    SectionLabel(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
        color = color,
    )
}

/** Last and apart: cards back to Cards left, the list out of Community, the list into the trash. */
@Composable
private fun PublicationGroup(
    list: TierList,
    published: Boolean,
    signedIn: Boolean,
    publishing: Boolean,
    publishError: PublishError?,
    publishedIsBehind: Boolean,
    awaitingApproval: Boolean,
    publicPending: Boolean?,
    onPublish: ((Publishing) -> Unit)?,
    onSetPublic: (Boolean) -> Unit,
    onUpdatePublished: () -> Unit,
    onAddCards: () -> Unit,
    onSeeInCommunity: (() -> Unit)?,
    onShareLink: (() -> Unit)?,
    onOpenOriginal: (() -> Unit)?,
    onChooseCategory: () -> Unit,
    onChooseTopic: () -> Unit,
    onChooseCover: () -> Unit,
    onAdjustCover: () -> Unit,
    onShare: () -> Unit,
) {
    GroupLabel(stringResource(R.string.list_settings_publication))
    PublishSection(
        list = list,
        published = published,
        signedIn = signedIn,
        busy = publishing,
        error = publishError,
        behind = publishedIsBehind,
        awaiting = awaitingApproval,
        onPublish = onPublish ?: { kind -> if (kind == Publishing.Update) onUpdatePublished() else onSetPublic(true) },
        onAddCards = onAddCards,
        // A ranking's original has its own row just under: no second button for it here.
        onOpenInCommunity = onSeeInCommunity.takeIf { list.rankingOf == null && publicPending != false },
        onShareLink = onShareLink.takeIf { publicPending != false },
    )
    if (onOpenOriginal != null) OpenOriginalRow(onClick = onOpenOriginal)

    // A category exists only for the feed; a cover is also the card's
    // own art, so it stays available to a guest.
    CategoryRow(category = list.category, onClick = onChooseCategory)
    // A topic lives inside a section: with none yet, the Section sheet comes first.
    TopicRow(
        topicName = list.topicName,
        newTopicName = list.newTopicName,
        onClick = { if (list.category == null) onChooseCategory() else onChooseTopic() },
    )
    CoverRows(list, onChooseCover = onChooseCover, onAdjustCover = onAdjustCover)
    // Beside the cover: sharing is not publishing and needs no account.
    ShareRow(onClick = onShare)
}

@Composable
private fun CarefulGroup(
    onReturnAllToPool: (() -> Unit)?,
    hasPlacedCards: Boolean,
    onTakeDown: (() -> Unit)?,
    takeDownEnabled: Boolean,
    onDelete: (() -> Unit)?,
) {
    if (onReturnAllToPool == null && onTakeDown == null && onDelete == null) return
    GroupLabel(stringResource(R.string.list_settings_group_careful), MaterialTheme.colorScheme.error)
    val plain = MaterialTheme.colorScheme.onSurface
    if (onReturnAllToPool != null) {
        CarefulRow(
            icon = { RestoreIcon(24.dp, it) },
            title = stringResource(ThemeR.string.community_return_all_to_pool),
            color = plain,
            onClick = onReturnAllToPool,
            enabled = hasPlacedCards,
            testTag = TierDetailTestTags.SETTINGS_RETURN_ALL,
        )
    }
    if (onTakeDown != null) {
        CarefulRow(
            icon = { CloudOffIcon(24.dp, it) },
            title = stringResource(R.string.list_settings_take_down),
            color = plain,
            onClick = onTakeDown,
            enabled = takeDownEnabled,
            testTag = TierDetailTestTags.SETTINGS_TAKE_DOWN,
        )
    }
    if (onDelete != null) {
        CarefulRow(
            icon = { DeleteIcon(24.dp, it) },
            title = stringResource(R.string.list_settings_delete),
            color = MaterialTheme.colorScheme.error,
            onClick = onDelete,
            testTag = TierDetailTestTags.SETTINGS_DELETE_LIST,
        )
    }
}

@Composable
private fun CarefulRow(
    icon: @Composable (Color) -> Unit,
    title: String,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
) {
    val shown = if (enabled) color else color.copy(alpha = DISABLED_ALPHA)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(shown)
        Text(
            text = title,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = shown,
        )
    }
}

@Composable
private fun DeleteListDialog(title: String, published: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TierDetailTestTags.DELETE_LIST_DIALOG),
        title = { Text(stringResource(R.string.list_settings_delete_title)) },
        text = {
            val body = stringResource(R.string.list_settings_delete_body, title)
            Text(if (published) body + " " + stringResource(R.string.list_settings_delete_public) else body)
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(TierDetailTestTags.DELETE_LIST_CONFIRM)) {
                Text(stringResource(R.string.list_settings_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(ThemeR.string.action_cancel)) } },
    )
}

@Composable
private fun NewTierRow(tierCount: Int, hasRoom: Boolean, onClick: () -> Unit) {
    SettingsActionRow(
        icon = { PlusIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.tier_list_settings_new_tier_title),
        // The reason stays in sight rather than a tap that only refuses.
        subtitle = if (hasRoom) {
            pluralStringResource(R.plurals.tier_list_settings_tier_count, tierCount, tierCount)
        } else {
            stringResource(R.string.tier_list_settings_tier_limit)
        },
        onClick = onClick,
        testTag = TierDetailTestTags.NEW_TIER_ROW,
        enabled = hasRoom,
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun TierListSettingsScreenLightPreview() = TierYourLifeTheme(false) {
    TierListSettingsScreenContent(
        list = previewTierList,
        onBack = {},
        onAddTier = { _, _, _, _ -> },
        onSetDisplayMode = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun TierListSettingsScreenDarkPreview() = TierYourLifeTheme(true) {
    TierListSettingsScreenContent(
        list = previewTierList,
        onBack = {},
        onAddTier = { _, _, _, _ -> },
        onSetDisplayMode = {},
    )
}

/**
 * One Publication row (Danylo, 16.11.2026). Before: a line saying what will
 * happen, then Publish or Show in Rankings, which go at once. Once out: the
 * status, then Update (filled while the board has moved on), View and Take down.
 */
@Composable
private fun PublishSection(
    list: TierList,
    published: Boolean,
    signedIn: Boolean,
    busy: Boolean,
    error: PublishError?,
    behind: Boolean,
    awaiting: Boolean,
    onPublish: (Publishing) -> Unit,
    onAddCards: () -> Unit,
    onOpenInCommunity: (() -> Unit)?,
    onShareLink: (() -> Unit)?,
) = PublishSection(
    published = published,
    asRankingOf = if (list.rankingOf != null) list.authorName.orEmpty() else null,
    signedIn = signedIn,
    hasItems = list.tiers.any { it.items.isNotEmpty() },
    busy = busy,
    error = error,
    kind = publishingOf(list, behind) ?: if (list.takenFrom != null) Publishing.ShareRanking else Publishing.Publish,
    onPublish = onPublish,
    onAddCards = onAddCards,
    behind = behind,
    awaiting = awaiting,
    onOpenInCommunity = onOpenInCommunity,
    onShareLink = onShareLink,
)

@Composable
private fun PublishSection(
    published: Boolean,
    /** The author of the list this board is out as a ranking of, when it is. */
    asRankingOf: String?,
    signedIn: Boolean,
    hasItems: Boolean,
    busy: Boolean,
    error: PublishError?,
    kind: Publishing,
    onPublish: (Publishing) -> Unit,
    onAddCards: () -> Unit,
    behind: Boolean,
    awaiting: Boolean = false,
    onOpenInCommunity: (() -> Unit)? = null,
    onShareLink: (() -> Unit)? = null,
) {
    Column(
        Modifier
            // An empty list: the button cannot go, and the row leads to the cards it is asking for.
            .then(if (!published && !hasItems) Modifier.clickable(onClick = onAddCards) else Modifier)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
    ) {
        if (published) {
            PublishedStatus(asRankingOf = asRankingOf, behind = behind)
            if (awaiting && asRankingOf == null) {
                Text(
                    text = stringResource(R.string.list_awaiting_approval),
                    modifier = Modifier.testTag(TierDetailTestTags.AWAITING_APPROVAL),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PublishedActions(
                behind = behind,
                busy = busy,
                calls = PublishedCalls(onUpdate = { onPublish(Publishing.Update) }, onShareLink, onOpenInCommunity),
            )
        } else {
            PublishLine(
                when {
                    !hasItems -> R.string.list_settings_public_needs_items
                    kind == Publishing.ShareRanking -> ThemeR.string.publication_preview_ranking
                    !signedIn -> R.string.list_settings_public_needs_account
                    else -> R.string.list_settings_public_body
                },
            )
            PublishButtonRow(kind, enabled = hasItems && !busy, onPublish = onPublish, tag = TierDetailTestTags.SETTINGS_PUBLISH)
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error.message(),
                modifier = Modifier.testTag(TierDetailTestTags.PUBLISH_ERROR),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (published) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.list_settings_public_images),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun PublishedStatus(asRankingOf: String?, behind: Boolean) {
    Text(
        text = stringResource(if (asRankingOf != null) R.string.list_settings_ranked_status else R.string.list_settings_published_status),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    if (asRankingOf != null) {
        Text(
            text = stringResource(R.string.list_settings_public_ranking_body, asRankingOf),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (behind) {
        Text(
            text = stringResource(R.string.list_settings_unpublished_changes),
            modifier = Modifier.testTag(TierDetailTestTags.UNPUBLISHED_CHANGES),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private class PublishedCalls(
    val onUpdate: () -> Unit,
    val onShareLink: (() -> Unit)?,
    val onOpenInCommunity: (() -> Unit)?,
)

/** Update (filled only when there is something to send) · Share · Open in Community; taking it down is under Careful. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PublishedActions(behind: Boolean, busy: Boolean, calls: PublishedCalls) {
    val onUpdate = calls.onUpdate
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        val update = Modifier.testTag(TierDetailTestTags.UPDATE_PUBLISHED)
        val padding = PaddingValues(start = 14.dp, end = 18.dp)
        val content: @Composable RowScope.() -> Unit = {
            PublishingIcon(Publishing.Update, LocalContentColor.current)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.board_publish_update))
        }
        if (behind) {
            Button(onClick = onUpdate, enabled = !busy, modifier = update, contentPadding = padding, content = content)
        } else {
            FilledTonalButton(onClick = onUpdate, enabled = !busy, modifier = update, contentPadding = padding, content = content)
        }
        calls.onShareLink?.let { share ->
            TextButton(onClick = share, modifier = Modifier.testTag(TierDetailTestTags.SETTINGS_SHARE_LINK)) {
                ShareIcon(18.dp, LocalContentColor.current)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share_button))
            }
        }
        calls.onOpenInCommunity?.let { open ->
            TextButton(onClick = open, modifier = Modifier.testTag(TierDetailTestTags.SETTINGS_OPEN_IN_COMMUNITY)) {
                OpenInNewIcon(18.dp, LocalContentColor.current)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.list_live_open))
            }
        }
    }
}

@Composable
private fun PublishLine(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PublishButtonRow(kind: Publishing, enabled: Boolean, onPublish: (Publishing) -> Unit, tag: String) {
    Spacer(Modifier.height(10.dp))
    Button(
        onClick = { onPublish(kind) },
        enabled = enabled,
        modifier = Modifier.testTag(tag),
        contentPadding = PaddingValues(start = 14.dp, end = 18.dp),
    ) {
        PublishingIcon(kind, LocalContentColor.current)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(publishLabel(kind)))
    }
}

@Composable
private fun PublishError.message(): String = publishErrorText(this, LocalResources.current, LocalConfiguration.current.locales[0])

/** Refusals put right in settings, so a publish from the bar opens them on the Publication row with the reason. */
fun PublishError.fixedInSettings(): Boolean = when (this) {
    PublishError.NothingToPublish,
    PublishError.PictureRefused,
    PublishError.PictureUnchecked,
    PublishError.SectionUnavailable,
    PublishError.TopicUnavailable,
    PublishError.TooManyPendingTopics,
    -> true
    else -> false
}

/** The sentence for a refused publish, wherever it is said: here, or beside the list among the person's public ones. */
fun publishErrorText(error: PublishError, resources: Resources, locale: Locale): String = when (error) {
    is PublishError.WeeklyLimit -> {
        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(error.retryAtMillis))
        resources.getString(ThemeR.string.publication_limit_until, date)
    }
    is PublishError.Banned -> {
        val until = error.untilMillis
        if (until == null) {
            resources.getString(R.string.list_settings_public_banned_forever)
        } else {
            val date = DateFormat.getDateInstance(DateFormat.LONG, locale).format(Date(until))
            resources.getString(R.string.list_settings_public_banned_until, date)
        }
    }
    else -> resources.getString(error.messageRes())
}

private fun PublishError.messageRes(): Int = when (this) {
    PublishError.NotSignedIn -> R.string.list_settings_public_needs_account
    PublishError.NothingToPublish -> R.string.list_settings_public_needs_items
    PublishError.TooManyLists -> R.string.list_settings_public_too_many
    is PublishError.WeeklyLimit -> ThemeR.string.publication_limit_until
    PublishError.ChangedElsewhere -> R.string.list_settings_public_changed_elsewhere
    PublishError.SourceChanged -> ThemeR.string.source_changed
    PublishError.TooManyPendingTopics -> R.string.list_settings_public_too_many_topics
    PublishError.TopicUnavailable -> R.string.list_settings_public_topic_unavailable
    PublishError.TooLarge -> R.string.list_settings_public_too_large
    PublishError.TooManyWebPictures -> ThemeR.string.refusal_too_many_web_pictures
    PublishError.WebPicturesToday -> ThemeR.string.refusal_web_pictures_today
    PublishError.ListGone -> R.string.list_settings_public_gone
    PublishError.SectionUnavailable -> R.string.list_settings_public_section_unavailable
    PublishError.RankInstead -> R.string.list_settings_public_rank_instead
    PublishError.NotTakenDown -> R.string.list_settings_unpublish_failed
    PublishError.PictureRefused -> R.string.list_settings_public_picture_refused
    PublishError.PictureUnchecked -> R.string.list_settings_public_picture_unchecked
    PublishError.PicturesCheckedToday -> ThemeR.string.refusal_pictures_checked_today
    is PublishError.Banned -> R.string.list_settings_public_banned_forever
    PublishError.Offline -> R.string.list_settings_public_offline
    PublishError.NotVerified -> R.string.list_settings_public_unverified
    PublishError.Unknown -> R.string.list_settings_public_failed
}
