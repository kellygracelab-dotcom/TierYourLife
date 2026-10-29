package com.artiuillab.tieryourlife.feature.community.presentation.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.BarredScreen
import com.artiuillab.tieryourlife.core.theme.layout.topBarHeight
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.ui.ActionFailedSnackbar
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.CommunityFeedList
import com.artiuillab.tieryourlife.feature.community.presentation.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportDialog
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportSentDialog
import com.artiuillab.tieryourlife.feature.community.presentation.feed.CommunityFeed
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private val SORT_HEIGHT = 36.dp

/**
 * One topic: what it is called, where it sits, how many stand behind it, and
 * its lists in the order the reader picks. Following it puts its lists into
 * the Following feed beside the people followed.
 */
@Composable
fun TopicScreen(
    onBack: () -> Unit,
    onOpenList: (CommunityRoute.CommunityList) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    viewModel: TopicViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TopicScreenContent(
        state = state,
        onBack = onBack,
        onOpenList = onOpenList,
        onAuthorClick = onAuthorClick,
        onToggleFollow = viewModel::toggleFollow,
        onSelectSort = viewModel::selectSort,
        onRetry = viewModel::load,
        onLoadMore = viewModel::loadMore,
        onHideList = viewModel::hideList,
        onReport = viewModel::report,
        onNotThisTopic = viewModel::notThisTopic,
        userMessages = viewModel.userMessages,
    )
}

@Composable
fun TopicScreenContent(
    state: TopicUiState,
    onBack: () -> Unit,
    onOpenList: (CommunityRoute.CommunityList) -> Unit = {},
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    onToggleFollow: () -> Unit = {},
    onSelectSort: (FeedSort) -> Unit = {},
    onRetry: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onHideList: (PublishedListSummary) -> Unit = {},
    onReport: (PublishedListSummary, ReportReason, String?) -> Unit = { _, _, _ -> },
    onNotThisTopic: (PublishedListSummary) -> Unit = {},
    userMessages: Flow<UserMessage> = emptyFlow(),
) {
    var actionsFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportedFrom by remember { mutableStateOf<PublishedListSummary?>(null) }
    // How many times "not this topic" was said; each one gets a word back, since nothing on screen changes.
    var told by remember { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val toldMessage = stringResource(R.string.topic_not_this_sent)
    LaunchedEffect(told) {
        if (told > 0) snackbar.showSnackbar(toldMessage)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        ActionFailedSnackbar(userMessages)
        Column(
            Modifier
                .fillMaxSize()
                .testTag(TopicTestTags.SCREEN),
        ) {
            BarredScreen(
                bar = {
                    TopBar(onBack = onBack, section = state.section)
                    Header(state = state, onToggleFollow = onToggleFollow)
                    SortRow(sort = state.sort, onSelectSort = onSelectSort)
                },
            ) {
                Column(Modifier.fillMaxSize()) {
                    CommunityFeedList(
                        feed = state.feed,
                        category = null,
                        onSelectCategory = {},
                        onOpen = { onOpenList(CommunityRoute.CommunityList.of(it)) },
                        onRetry = onRetry,
                        onOpenAuthor = { uid ->
                            val summary = (state.feed as? CommunityFeed.Ready)?.lists?.firstOrNull { it.authorUid == uid }
                            if (summary != null) onAuthorClick(uid, summary.authorName, summary.authorPhotoUrl)
                        },
                        onLongPress = { actionsFor = it },
                        onNearEnd = onLoadMore,
                        showCategories = false,
                    )
                }
            }
        }

        actionsFor?.let { summary ->
            ListActionsSheet(
                title = summary.title,
                authorName = summary.authorName.takeUnless { summary.editorial },
                authorPhotoUrl = summary.authorPhotoUrl,
                onDismiss = { actionsFor = null },
                onOpenAuthor = if (summary.editorial) {
                    null
                } else {
                    {
                        actionsFor = null
                        onAuthorClick(summary.authorUid, summary.authorName, summary.authorPhotoUrl)
                    }
                },
                onHide = {
                    actionsFor = null
                    onHideList(summary)
                }.takeUnless { summary.mine },
                onReport = {
                    actionsFor = null
                    reportFor = summary
                }.takeUnless { summary.mine },
                onWrongTopic = {
                    actionsFor = null
                    onNotThisTopic(summary)
                    told++
                },
            )
        }

        reportFor?.let { summary ->
            ReportDialog(
                onDismiss = { reportFor = null },
                onSend = { reason, note ->
                    reportFor = null
                    onReport(summary, reason, note)
                    reportedFrom = summary
                },
            )
        }

        reportedFrom?.let { summary ->
            ReportSentDialog(
                authorName = summary.authorName.takeUnless { summary.editorial },
                onDismiss = { reportedFrom = null },
                onHideAuthor = { reportedFrom = null },
            )
        }

        Box(Modifier.fillMaxSize()) {
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Header(state: TopicUiState, onToggleFollow: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = state.name,
            modifier = Modifier.testTag(TopicTestTags.NAME),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val topic = state.topic
        if (topic != null) {
            Text(
                text = pluralStringResource(R.plurals.topic_lists, topic.listCount, topic.listCount) + " · " +
                    pluralStringResource(R.plurals.topic_followers, topic.followerCount, topic.followerCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (topic.status == TopicStatus.Pending) {
                Text(
                    text = stringResource(R.string.topic_waiting),
                    modifier = Modifier.testTag(TopicTestTags.WAITING),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            // Only an approved word can be followed; the server refuses the rest.
            if (topic.status == TopicStatus.Approved) {
                if (topic.following) {
                    OutlinedButton(onClick = onToggleFollow, modifier = Modifier.testTag(TopicTestTags.FOLLOW)) {
                        Text(stringResource(R.string.action_unfollow))
                    }
                } else {
                    Button(onClick = onToggleFollow, modifier = Modifier.testTag(TopicTestTags.FOLLOW)) {
                        Text(stringResource(R.string.action_follow))
                    }
                }
            }
        } else if (state.headerFailed) {
            Text(
                text = stringResource(R.string.topic_header_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SortRow(sort: FeedSort, onSelectSort: (FeedSort) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(SORT_HEIGHT),
    ) {
        val options = listOf(FeedSort.Popular, FeedSort.Recent)
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = sort == option,
                onClick = { onSelectSort(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.testTag(TopicTestTags.sort(option.id)),
            ) {
                Text(stringResource(if (option == FeedSort.Popular) R.string.feed_sort_popular else R.string.feed_sort_recent))
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, section: String?) {
    val backDescription = stringResource(ThemeR.string.tier_detail_content_description_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(topBarHeight())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = backDescription }
                .testTag(TopicTestTags.BACK),
        ) { BackIcon() }
        // The section is the small word above the name, as on the canvas.
        Text(
            text = section?.let { sectionLabel(it) } ?: stringResource(R.string.topic_title),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_9")
@Composable
private fun TopicPreview() = TierYourLifeTheme {
    TopicScreenContent(
        state = TopicUiState(
            name = "Pokémon",
            section = ListCategory.Games.id,
            topic = Topic(id = "t1", name = "Pokémon", section = ListCategory.Games.id, status = TopicStatus.Approved, listCount = 212, followerCount = 1340),
            feed = CommunityFeed.Ready(emptyList()),
        ),
        onBack = {},
    )
}
