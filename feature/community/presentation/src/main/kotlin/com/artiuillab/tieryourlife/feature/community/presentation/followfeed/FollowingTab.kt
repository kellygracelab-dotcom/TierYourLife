package com.artiuillab.tieryourlife.feature.community.presentation.followfeed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.core.theme.ui.TuneIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorFace
import com.artiuillab.tieryourlife.feature.community.presentation.explore.AllSectionsTile
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CardActions
import com.artiuillab.tieryourlife.feature.community.presentation.explore.DashedSquare
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ExploreTestTags
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.explore.GroupIcon
import com.artiuillab.tieryourlife.feature.community.presentation.explore.HomeLayout
import com.artiuillab.tieryourlife.feature.community.presentation.explore.NearEndWatcher
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ScheduleIcon
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ScrolledDownWatcher
import com.artiuillab.tieryourlife.feature.community.presentation.explore.SectionTile
import com.artiuillab.tieryourlife.feature.community.presentation.explore.TileRows
import com.artiuillab.tieryourlife.feature.community.presentation.explore.TileStyle
import com.artiuillab.tieryourlife.feature.community.presentation.explore.WideContentMax
import com.artiuillab.tieryourlife.feature.community.presentation.explore.feedItems
import com.artiuillab.tieryourlife.feature.community.presentation.explore.rememberSkeletonAlpha
import com.artiuillab.tieryourlife.feature.community.presentation.section.ProposalNotices
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionArt
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionIcon
import com.artiuillab.tieryourlife.feature.community.presentation.section.WaitingSectionSquare
import com.artiuillab.tieryourlife.feature.community.presentation.section.lookOf
import com.artiuillab.tieryourlife.feature.community.presentation.section.topicBgNow
import com.artiuillab.tieryourlife.feature.community.presentation.section.topicIconNow
import com.artiuillab.tieryourlife.feature.tier.board.common.LocalSections
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.common.rememberSection
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

/** Where the strip and the empty tab lead; sections go by id. */
internal class FollowingActions(
    val onOpenSection: (String) -> Unit = {},
    val onOpenTopic: (Topic) -> Unit = {},
    val onOpenWaiting: (Topic) -> Unit = {},
    val onOpenAuthor: (FollowedAuthor) -> Unit = {},
    val onManage: () -> Unit = {},
    val onQuickFollow: (String) -> Unit = {},
    val onGoToExplore: () -> Unit = {},
    val onOpenAllSections: () -> Unit = {},
    val onOpenWaitingSection: (Section) -> Unit = {},
    val onDismissNotice: (String) -> Unit = {},
)

/** The Following tab (§4): what is followed, then everything new from it, newest first, each card saying why. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FollowingTab(
    state: FollowingFeedUiState,
    layout: HomeLayout,
    gridState: LazyGridState,
    actions: FollowingActions,
    cards: CardActions,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onScrolledDown: (Boolean) -> Unit,
) {
    val skeletonAlpha = rememberSkeletonAlpha()
    ScrolledDownWatcher(gridState, onScrolledDown)
    NearEndWatcher(gridState, state.feed.lists.size, onLoadMore)
    PullToRefreshBox(isRefreshing = state.feed.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(layout.feedColumns),
                state = gridState,
                modifier = Modifier
                    .then(if (layout == HomeLayout.Wide) Modifier.widthIn(max = WideContentMax) else Modifier)
                    .fillMaxSize(),
                contentPadding = systemBarsContentPadding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.followsNothing) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        FollowsNothing(state.quickFollowed, actions)
                    }
                    return@LazyVerticalGrid
                }
                val following = state.following
                if (state.proposalNotices.isNotEmpty()) {
                    item(key = "notices", span = { GridItemSpan(maxLineSpan) }) {
                        ProposalNotices(state.proposalNotices, onDismiss = actions.onDismissNotice, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                if (following != null) {
                    item(key = "strip", span = { GridItemSpan(maxLineSpan) }) {
                        FollowedStrip(following, state.waiting, actions, state.waitingSections)
                    }
                }
                if (state.feed.phase == FeedPhase.Ready && state.feed.lists.isEmpty()) {
                    item(key = "nothing", span = { GridItemSpan(maxLineSpan) }) {
                        NothingPublished(actions.onGoToExplore)
                    }
                } else {
                    feedItems(state.feed, layout, skeletonAlpha, cards, onRetry) { summary ->
                        following?.let { reasonFor(summary, it) }?.let { reasonText(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun reasonText(reason: FeedReason): String = when (reason) {
    is FeedReason.InTopic -> reason.name
    is FeedReason.InSection -> stringResource(R.string.following_reason_section, sectionLabel(reason.sectionId))
}

private val StripLabel = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
private val StripVisual = 56.dp

/** Sections, topics (waiting ones last), people, then Manage (§4.1). */
@Composable
internal fun FollowedStrip(following: Following, waiting: List<Topic>, actions: FollowingActions, waitingSections: List<Section> = emptyList()) {
    Column(Modifier.fillMaxWidth().testTag(ExploreTestTags.STRIP)) {
        LazyRow(
            contentPadding = PaddingValues(top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            following.sections.forEach { followed ->
                item(key = "s-${followed.id}") { FollowedSectionItem(followed, actions) }
            }
            waitingSections.forEach { section ->
                item(key = "ws-${section.id}") {
                    StripItem(section.label(), "ws-${section.id}", { actions.onOpenWaitingSection(section) }) { WaitingSectionSquare(section, StripVisual) }
                }
            }
            following.topics.forEach { topic ->
                item(key = "t-${topic.id}") {
                    StripItem(topic.name, "t-${topic.id}", { actions.onOpenTopic(topic) }) { TopicSquare(topic.section, StripVisual, 22.dp, topic.pictureUrl) }
                }
            }
            waiting.forEach { topic ->
                item(key = "w-${topic.id}") {
                    StripItem(topic.name, "w-${topic.id}", { actions.onOpenWaiting(topic) }) {
                        Box(Modifier.size(StripVisual), contentAlignment = Alignment.Center) {
                            DashedSquare(StripVisual, MaterialTheme.colorScheme.outline, corner = 6f)
                            ScheduleIcon(22.dp, MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
            following.authors.forEach { author ->
                item(key = "p-${author.uid}") {
                    StripItem(author.name, "p-${author.uid}", { actions.onOpenAuthor(author) }) {
                        AuthorFace(photoUrl = author.photoUrl, name = author.name, size = StripVisual)
                    }
                }
            }
            item(key = "manage") {
                StripItem(stringResource(R.string.following_strip_manage), "manage", actions.onManage, labelColor = MaterialTheme.colorScheme.primary) {
                    Box(
                        Modifier
                            .size(StripVisual)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        TuneIcon(22.dp, MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun FollowedSectionItem(followed: FollowedSection, actions: FollowingActions) {
    val section = rememberSection(followed.id)
    StripItem(section.label(), "s-${followed.id}", { actions.onOpenSection(followed.id) }) {
        Box(Modifier.size(StripVisual).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            SectionArt(section, Modifier.fillMaxSize(), scrim = false)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            SectionIcon(section, 22.dp, Color.White)
        }
    }
}

/**
 * A topic's square: its own picture, cropped, once it has one; until then,
 * and under it while it loads, its section's sign on the section's pale tint,
 * Other's while it has no section.
 */
@Composable
internal fun TopicSquare(sectionId: String?, size: Dp, icon: Dp, pictureUrl: String? = null) {
    val placed = rememberSection(sectionId ?: ListCategory.Other.id)
    val look = lookOf(placed)
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(if (size > 40.dp) 14.dp else 12.dp))
            .background(look.topicBgNow)
            .testTag(ExploreTestTags.TOPIC_SQUARE),
        contentAlignment = Alignment.Center,
    ) {
        SectionIcon(placed, icon, look.topicIconNow)
        if (pictureUrl != null) {
            AsyncImage(
                model = pictureUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().testTag(ExploreTestTags.TOPIC_PICTURE),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun StripItem(
    label: String,
    key: String,
    onClick: () -> Unit,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    visual: @Composable () -> Unit,
) {
    Column(
        Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(ExploreTestTags.stripItem(key)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        visual()
        Text(
            text = label,
            modifier = Modifier.padding(top = 6.dp),
            style = StripLabel,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Nothing followed at all (§4.2): the sections, each one tap from being
 * followed; nine at most, the last leading to all of them when there are more.
 */
@Composable
private fun FollowsNothing(quickFollowed: Set<String>, actions: FollowingActions) {
    val shown = LocalSections.current.directory.shown
    val more = shown.size > MINI_SLOTS
    val slots = if (more) shown.filterNot { it.isOther }.take(MINI_SLOTS - 1) + null else shown
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, start = 8.dp, end = 8.dp)
            .testTag(ExploreTestTags.FOLLOWING_EMPTY),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GroupIcon(36.dp, MaterialTheme.colorScheme.outline)
        Text(
            stringResource(R.string.following_empty_title),
            modifier = Modifier.padding(top = 4.dp),
            style = TextStyle(fontSize = 20.sp, lineHeight = 26.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.following_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TileRows(slots, columns = 3, gap = 8.dp) { section, rowModifier ->
                if (section == null) {
                    AllSectionsTile(
                        count = shown.size,
                        onClick = actions.onOpenAllSections,
                        modifier = rowModifier.height(84.dp).testTag(ExploreTestTags.ALL_SECTIONS_MINI_TILE),
                        style = TileStyle.Mini,
                    )
                } else {
                    MiniTile(section, following = section.id in quickFollowed, onFollow = { actions.onQuickFollow(section.id) }, modifier = rowModifier)
                }
            }
        }
        Text(
            stringResource(R.string.following_empty_helper),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val MINI_SLOTS = 9

@Composable
private fun MiniTile(section: Section, following: Boolean, onFollow: () -> Unit, modifier: Modifier) {
    Box(modifier.height(84.dp)) {
        SectionTile(
            section = section,
            newThisWeek = null,
            following = following,
            onOpen = onFollow,
            onFollow = onFollow,
            onUnfollow = {},
            modifier = Modifier.fillMaxSize().testTag(ExploreTestTags.miniTile(section.id)),
            style = TileStyle.Mini,
            longPressMenu = false,
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (following) {
                CheckIcon(18.dp, MaterialTheme.colorScheme.primary)
            } else {
                PlusIcon(18.dp, MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Following things, none of which has published anything yet. */
@Composable
private fun NothingPublished(onGoToExplore: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(28.dp)
            .testTag(ExploreTestTags.FOLLOWING_NOTHING_PUBLISHED),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.following_nothing_published),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onGoToExplore) { Text(stringResource(R.string.following_go_explore)) }
    }
}
