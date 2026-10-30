package com.artiuillab.tieryourlife.feature.community.presentation.following

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.BarredScreen
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.layout.topBarHeight
import com.artiuillab.tieryourlife.core.theme.layout.topBarTitleStyle
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorFace
import com.artiuillab.tieryourlife.feature.community.presentation.explore.DashedSquare
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FollowSize
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FollowToggle
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ScheduleIcon
import com.artiuillab.tieryourlife.feature.community.presentation.explore.countText
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.TopicSquare
import com.artiuillab.tieryourlife.feature.community.presentation.section.SectionIcon
import com.artiuillab.tieryourlife.feature.community.presentation.section.lookOf
import com.artiuillab.tieryourlife.feature.community.presentation.section.tintNow
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.common.rememberSection
import com.artiuillab.tieryourlife.feature.tier.board.common.rememberSectionNames
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** Manage following (§4.3): everything followed, grouped, each with the one thing to do here: stop, and start again. */
@Composable
fun FollowingScreen(
    onBack: () -> Unit,
    onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit,
    onOpenTopic: (Topic) -> Unit,
    onOpenSection: (String) -> Unit = {},
    viewModel: FollowingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FollowingScreenContent(
        state = state,
        onBack = onBack,
        onOpenAuthor = onOpenAuthor,
        onToggle = viewModel::toggle,
        onRetry = viewModel::load,
        onOpenTopic = onOpenTopic,
        onOpenSection = onOpenSection,
        notices = viewModel.notices,
    )
}

@Composable
fun FollowingScreenContent(
    state: FollowingUiState,
    onBack: () -> Unit,
    onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit,
    onToggle: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTopic: (Topic) -> Unit = {},
    onOpenSection: (String) -> Unit = {},
    notices: Flow<ManageNotice> = emptyFlow(),
) {
    val snackbar = remember { SnackbarHostState() }
    ManageSnackbars(notices, snackbar, onToggle)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .testTag(FollowingTestTags.SCREEN),
            ) {
                BarredScreen(
                    bar = {
                        TopBar(onBack = onBack)
                    },
                ) {
                    Column(Modifier.fillMaxSize()) {
                        when (state) {
                            FollowingUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }

                            FollowingUiState.Failed -> Message(
                                text = stringResource(R.string.home_community_failed),
                                action = stringResource(ThemeR.string.action_try_again),
                                onAction = onRetry,
                                testTag = FollowingTestTags.FAILED,
                            )

                            is FollowingUiState.Ready -> if (state.isEmpty) {
                                Message(text = stringResource(R.string.community_following_nobody), testTag = FollowingTestTags.EMPTY)
                            } else {
                                LazyColumn(contentPadding = systemBarsContentPadding(bottom = 16.dp)) {
                                    sectionsBlock(state, onToggle, onOpenSection)
                                    topicsBlock(state, onToggle, onOpenTopic)
                                    peopleBlock(state, onToggle, onOpenAuthor)
                                }
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ManageSnackbars(notices: Flow<ManageNotice>, snackbar: SnackbarHostState, onToggle: (String) -> Unit) {
    val resources = LocalResources.current
    val nameOf = rememberSectionNames()
    LaunchedEffect(notices) {
        notices.collect { notice ->
            when (notice) {
                is ManageNotice.Stopped -> {
                    val shown = if (notice.key.startsWith("s:")) {
                        nameOf(notice.name)
                    } else {
                        notice.name
                    }
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.explore_unfollowed, shown),
                        actionLabel = resources.getString(R.string.explore_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) onToggle(notice.key)
                }

                ManageNotice.Failed -> snackbar.showSnackbar(resources.getString(ThemeR.string.snack_action_failed))
            }
        }
    }
}

private fun LazyListScope.sectionsBlock(state: FollowingUiState.Ready, onToggle: (String) -> Unit, onOpen: (String) -> Unit) {
    if (state.sections.isEmpty()) return
    item(key = "sections") { Heading(R.string.manage_sections, state.sections.size) }
    items(state.sections, key = { FollowKeys.section(it.id) }) { followed ->
        val key = FollowKeys.section(followed.id)
        ManageRow(
            key = key,
            headline = rememberSection(followed.id).label(),
            supporting = countText(R.plurals.explore_followers, followed.followerCount),
            following = key !in state.stopped,
            onOpen = { onOpen(followed.id) },
            onToggle = { onToggle(key) },
        ) { SectionSquare(followed) }
    }
}

private fun LazyListScope.topicsBlock(state: FollowingUiState.Ready, onToggle: (String) -> Unit, onOpen: (Topic) -> Unit) {
    if (state.topics.isEmpty() && state.waiting.isEmpty()) return
    item(key = "topics") { Heading(R.string.manage_topics, state.topics.size + state.waiting.size) }
    items(state.topics, key = { FollowKeys.topic(it.id) }) { topic ->
        val key = FollowKeys.topic(topic.id)
        val section = topic.section ?: ListCategory.Other.id
        ManageRow(
            key = key,
            headline = topic.name,
            supporting = stringResource(R.string.manage_topic_supporting, sectionLabel(section), countText(R.plurals.explore_followers, topic.followerCount)),
            following = key !in state.stopped,
            onOpen = { onOpen(topic) },
            onToggle = { onToggle(key) },
        ) { TopicSquare(section, 48.dp, 20.dp, topic.pictureUrl) }
    }
    items(state.waiting, key = { "w:" + it.id }) { topic ->
        val section = topic.section ?: ListCategory.Other.id
        ManageRow(
            key = "w:" + topic.id,
            headline = topic.name,
            supporting = stringResource(R.string.manage_waiting_supporting, sectionLabel(section)),
            following = null,
            onOpen = {},
            onToggle = {},
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                DashedSquare(48.dp, MaterialTheme.colorScheme.outline, corner = 6f)
                ScheduleIcon(20.dp, MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun LazyListScope.peopleBlock(
    state: FollowingUiState.Ready,
    onToggle: (String) -> Unit,
    onOpen: (uid: String, name: String, photoUrl: String?) -> Unit,
) {
    if (state.authors.isEmpty()) return
    item(key = "people") { Heading(R.string.manage_people, state.authors.size) }
    items(state.authors, key = { FollowKeys.person(it.uid) }) { person ->
        val key = FollowKeys.person(person.uid)
        ManageRow(
            key = key,
            // An author whose lists are all gone keeps the follow and loses the name.
            headline = person.name.ifEmpty { stringResource(R.string.community_author_unnamed) },
            supporting = null,
            following = key !in state.stopped,
            onOpen = { onOpen(person.uid, person.name, person.photoUrl) },
            onToggle = { onToggle(key) },
        ) { AuthorFace(photoUrl = person.photoUrl, name = person.name.ifEmpty { null }, size = 48.dp) }
    }
}

@Composable
private fun SectionSquare(followed: FollowedSection) {
    val section = rememberSection(followed.id)
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(lookOf(section).tintNow),
        contentAlignment = Alignment.Center,
    ) {
        SectionIcon(section, 20.dp, Color.White)
    }
}

@Composable
private fun Heading(label: Int, count: Int) {
    Text(
        text = stringResource(label, count),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** A two-line row, 72dp: who or what, a line about it, and Following / Follow (none for a waiting topic). */
@Composable
private fun ManageRow(
    key: String,
    headline: String,
    supporting: String?,
    following: Boolean?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp)
            .testTag(FollowingTestTags.row(key)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (following != null) {
            FollowToggle(
                following = following,
                onClick = onToggle,
                size = FollowSize.Compact,
                modifier = Modifier.testTag(FollowingTestTags.toggle(key)),
            )
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
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
                .testTag(FollowingTestTags.BACK),
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.action_following),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = topBarTitleStyle(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Message(text: String, testTag: String, action: String? = null, onAction: () -> Unit = {}) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .atMost(ContentWidth.Message)
                .testTag(testTag),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_9")
@Composable
private fun FollowingPreview() = TierYourLifeTheme {
    FollowingScreenContent(
        state = FollowingUiState.Ready(
            authors = listOf(FollowedAuthor(uid = "a", name = "Olena M.", photoUrl = null)),
            topics = listOf(Topic(id = "t1", name = "Pokémon", section = ListCategory.Games.id, status = TopicStatus.Approved, followerCount = 210)),
            sections = listOf(FollowedSection(ListCategory.Food.id, followerCount = 4120)),
            waiting = listOf(Topic(id = "t2", name = "Shiny hunts", section = ListCategory.Games.id, status = TopicStatus.Pending)),
            stopped = setOf(FollowKeys.person("a")),
        ),
        onBack = {},
        onOpenAuthor = { _, _, _ -> },
        onToggle = {},
        onRetry = {},
    )
}
