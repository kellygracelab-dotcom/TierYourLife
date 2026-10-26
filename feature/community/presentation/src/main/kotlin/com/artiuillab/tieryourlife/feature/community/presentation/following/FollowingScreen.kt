package com.artiuillab.tieryourlife.feature.community.presentation.following

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.artiuillab.tieryourlife.core.theme.layout.topBarHeight
import com.artiuillab.tieryourlife.core.theme.layout.topBarTitleStyle
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.ui.ActionFailedSnackbar
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.core.theme.ui.SectionLabel
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorFace
import com.artiuillab.tieryourlife.feature.tier.board.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private val FACE_SIZE = 40.dp

/** Everyone and everything this person follows, each with the one thing to do about it here: stop. */
@Composable
fun FollowingScreen(
    onBack: () -> Unit,
    onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit,
    onOpenTopic: (Topic) -> Unit,
    viewModel: FollowingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FollowingScreenContent(
        state = state,
        onBack = onBack,
        onOpenAuthor = onOpenAuthor,
        onUnfollow = viewModel::unfollow,
        onRetry = viewModel::load,
        onOpenTopic = onOpenTopic,
        onUnfollowTopic = viewModel::unfollowTopic,
        userMessages = viewModel.userMessages,
    )
}

@Composable
fun FollowingScreenContent(
    state: FollowingUiState,
    onBack: () -> Unit,
    onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit,
    onUnfollow: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTopic: (Topic) -> Unit = {},
    onUnfollowTopic: (String) -> Unit = {},
    userMessages: Flow<UserMessage> = emptyFlow(),
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        ActionFailedSnackbar(userMessages)
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

                        is FollowingUiState.Ready -> if (state.authors.isEmpty() && state.topics.isEmpty()) {
                            Message(text = stringResource(R.string.community_following_nobody), testTag = FollowingTestTags.EMPTY)
                        } else {
                            Followed(
                                state = state,
                                onOpenAuthor = onOpenAuthor,
                                onUnfollow = onUnfollow,
                                onOpenTopic = onOpenTopic,
                                onUnfollowTopic = onUnfollowTopic,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Followed(
    state: FollowingUiState.Ready,
    onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit,
    onUnfollow: (String) -> Unit,
    onOpenTopic: (Topic) -> Unit,
    onUnfollowTopic: (String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        // Headed only once there are two kinds; one kind alone needs no name.
        if (state.authors.isNotEmpty() && state.topics.isNotEmpty()) {
            item(key = "people") { Heading(stringResource(R.string.following_people)) }
        }
        items(state.authors, key = { "person-" + it.uid }) { person ->
            FollowedRow(
                person = person,
                onOpen = { onOpenAuthor(person.uid, person.name, person.photoUrl) },
                onUnfollow = { onUnfollow(person.uid) },
            )
        }
        if (state.topics.isNotEmpty()) {
            item(key = "topics") {
                Heading(stringResource(R.string.following_topics), modifier = Modifier.testTag(FollowingTestTags.TOPICS))
            }
        }
        items(state.topics, key = { "topic-" + it.id }) { topic ->
            TopicRow(
                topic = topic,
                onOpen = { onOpenTopic(topic) },
                onUnfollow = { onUnfollowTopic(topic.id) },
            )
        }
    }
}

@Composable
private fun Heading(text: String, modifier: Modifier = Modifier) {
    SectionLabel(text = text, modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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
private fun FollowedRow(person: FollowedAuthor, onOpen: () -> Unit, onUnfollow: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(FollowingTestTags.row(person.uid)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AuthorFace(photoUrl = person.photoUrl, name = person.name.ifEmpty { null }, size = FACE_SIZE)
        Text(
            // An author whose lists are all gone keeps the follow and loses the name.
            text = person.name.ifEmpty { stringResource(R.string.community_author_unnamed) },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        OutlinedButton(onClick = onUnfollow, modifier = Modifier.testTag(FollowingTestTags.unfollow(person.uid))) {
            Text(stringResource(R.string.action_unfollow))
        }
    }
}

@Composable
private fun TopicRow(topic: Topic, onOpen: () -> Unit, onUnfollow: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(FollowingTestTags.topicRow(topic.id)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = topic.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The shelf it sits on, or that it has none yet.
            Text(
                text = topic.section?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.topic_waiting_short),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onUnfollow, modifier = Modifier.testTag(FollowingTestTags.unfollowTopic(topic.id))) {
            Text(stringResource(R.string.action_unfollow))
        }
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
            authors = listOf(
                FollowedAuthor(uid = "a", name = "Olena M.", photoUrl = null),
                FollowedAuthor(uid = "b", name = "", photoUrl = null),
            ),
            topics = listOf(
                Topic(id = "t1", name = "Pokémon", section = ListCategory.Games, status = TopicStatus.Approved),
                Topic(id = "t2", name = "Kharkiv cafés", section = null, status = TopicStatus.Pending),
            ),
        ),
        onBack = {},
        onOpenAuthor = { _, _, _ -> },
        onUnfollow = {},
        onRetry = {},
    )
}
