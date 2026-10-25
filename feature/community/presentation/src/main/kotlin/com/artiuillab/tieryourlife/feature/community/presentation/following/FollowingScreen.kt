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
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.ui.ActionFailedSnackbar
import com.artiuillab.tieryourlife.core.theme.ui.BackIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorFace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private val FACE_SIZE = 40.dp

/** Everyone this person follows, each with the one thing to do about them here: stop. */
@Composable
fun FollowingScreen(
    onBack: () -> Unit,
    onOpenAuthor: (uid: String, name: String, photoUrl: String?) -> Unit,
    viewModel: FollowingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FollowingScreenContent(
        state = state,
        onBack = onBack,
        onOpenAuthor = onOpenAuthor,
        onUnfollow = viewModel::unfollow,
        onRetry = viewModel::load,
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
    userMessages: Flow<UserMessage> = emptyFlow(),
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        ActionFailedSnackbar(userMessages)
        Column(
            Modifier
                .fillMaxSize()
                .testTag(FollowingTestTags.SCREEN),
        ) {
            TopBar(onBack = onBack)
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

                is FollowingUiState.Ready -> if (state.authors.isEmpty()) {
                    Message(text = stringResource(R.string.community_following_nobody), testTag = FollowingTestTags.EMPTY)
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(state.authors, key = { it.uid }) { person ->
                            FollowedRow(
                                person = person,
                                onOpen = { onOpenAuthor(person.uid, person.name, person.photoUrl) },
                                onUnfollow = { onUnfollow(person.uid) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val backDescription = stringResource(ThemeR.string.tier_detail_content_description_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
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
            style = MaterialTheme.typography.titleLarge,
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
            listOf(
                FollowedAuthor(uid = "a", name = "Olena M.", photoUrl = null),
                FollowedAuthor(uid = "b", name = "", photoUrl = null),
            ),
        ),
        onBack = {},
        onOpenAuthor = { _, _, _ -> },
        onUnfollow = {},
        onRetry = {},
    )
}
