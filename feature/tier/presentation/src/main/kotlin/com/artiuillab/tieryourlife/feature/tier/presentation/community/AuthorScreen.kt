package com.artiuillab.tieryourlife.feature.tier.presentation.community

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.MoreIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.AuthorActionsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.AuthorFace
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ReportDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ReportSentDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.CommunityFeed
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.CommunityFeedList

private val PORTRAIT_SIZE = 80.dp

@Composable
fun AuthorScreen(
    onBack: () -> Unit,
    onOpenList: (String) -> Unit,
    viewModel: AuthorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AuthorScreenContent(
        state = state,
        onBack = onBack,
        onOpenList = onOpenList,
        onRetry = viewModel::load,
        onHideList = viewModel::hideList,
        onHideAuthor = {
            viewModel.hideAuthor()
            onBack()
        },
        onReport = viewModel::report,
    )
}

@Composable
internal fun AuthorScreenContent(
    state: AuthorUiState,
    onBack: () -> Unit,
    onOpenList: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onHideList: (String) -> Unit = {},
    onHideAuthor: () -> Unit = {},
    onReport: (String, ReportReason, String?) -> Unit = { _, _, _ -> },
) {
    var authorActionsVisible by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportFor by remember { mutableStateOf<PublishedListSummary?>(null) }
    var reportedFrom by remember { mutableStateOf<PublishedListSummary?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().testTag(AuthorTestTags.SCREEN)) {
            AuthorTopBar(
                onBack = onBack,
                onMoreClick = if (state is AuthorUiState.Ready) {
                    { authorActionsVisible = true }
                } else {
                    null
                },
            )

            when (state) {
                AuthorUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                AuthorUiState.Failed -> Message(
                    title = stringResource(R.string.author_failed),
                    body = stringResource(R.string.home_community_failed_body),
                    action = stringResource(R.string.action_try_again),
                    onAction = onRetry,
                )

                is AuthorUiState.Ready -> {
                    Header(state)
                    if (state.lists.isEmpty()) {
                        Message(
                            title = stringResource(R.string.author_no_public_lists),
                            // Answers the question a reader actually has: what
                            // happens to the copy they already took.
                            body = stringResource(R.string.author_no_public_lists_body),
                            action = null,
                            onAction = {},
                        )
                    } else {
                        CommunityFeedList(
                            feed = CommunityFeed.Ready(state.lists),
                            category = null,
                            onSelectCategory = {},
                            onOpen = onOpenList,
                            onRetry = onRetry,
                            onLongPress = { summary -> actionsFor = summary },
                            showCategories = false,
                            showAuthor = false,
                        )
                    }
                }
            }
        }

        val ready = state as? AuthorUiState.Ready
        if (ready != null && authorActionsVisible) {
            AuthorActionsSheet(
                name = ready.name,
                photoUrl = ready.photoUrl,
                onDismiss = { authorActionsVisible = false },
                onHideAuthor = {
                    authorActionsVisible = false
                    onHideAuthor()
                },
            )
        }

        actionsFor?.let { summary ->
            ListActionsSheet(
                title = summary.title,
                authorName = summary.authorName,
                authorPhotoUrl = summary.authorPhotoUrl,
                onDismiss = { actionsFor = null },
                // Already on their profile: there is nowhere else to go.
                onOpenAuthor = null,
                onHide = {
                    actionsFor = null
                    onHideList(summary.id)
                },
                onReport = {
                    actionsFor = null
                    reportFor = summary
                },
            )
        }

        reportFor?.let { summary ->
            ReportDialog(
                onDismiss = { reportFor = null },
                onSend = { reason, note ->
                    reportFor = null
                    onReport(summary.id, reason, note)
                    reportedFrom = summary
                },
            )
        }

        reportedFrom?.let { summary ->
            ReportSentDialog(
                authorName = summary.authorName,
                onDismiss = { reportedFrom = null },
                onHideAuthor = {
                    reportedFrom = null
                    onHideAuthor()
                },
            )
        }
    }
}

@Composable
private fun AuthorTopBar(onBack: () -> Unit, onMoreClick: (() -> Unit)?) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
    val moreDescription = stringResource(R.string.tier_detail_content_description_more)
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
            modifier = Modifier.size(48.dp).semantics { contentDescription = backDescription },
        ) { BackIcon() }
        Spacer(Modifier.weight(1f))
        if (onMoreClick != null) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = moreDescription }
                    .testTag(AuthorTestTags.MORE),
            ) { MoreIcon() }
        }
    }
}

@Composable
private fun Header(state: AuthorUiState.Ready) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthorFace(photoUrl = state.photoUrl, name = state.name, size = PORTRAIT_SIZE)
        Spacer(Modifier.height(12.dp))
        Text(
            text = state.name,
            modifier = Modifier.testTag(AuthorTestTags.NAME),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(R.plurals.author_public_lists, state.lists.size, state.lists.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Message(title: String, body: String, action: String?, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 96.dp)
            .testTag(AuthorTestTags.MESSAGE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

internal object AuthorTestTags {
    const val SCREEN = "author_screen"
    const val NAME = "author_name"
    const val MESSAGE = "author_message"
    const val MORE = "author_more"
    const val ACTIONS_SHEET = "author_actions"
    const val ACTION_HIDE_AUTHOR = "author_action_hide_author"
}

private val previewLists = listOf(
    PublishedListSummary(
        id = "1",
        title = "Every A24 film",
        authorUid = "u1",
        authorName = "Danylo K.",
        category = ListCategory.FilmTv,
        itemCount = 34,
        tierColors = listOf("#B03A32", "#C06A25", "#B79A1F"),
        updatedAtMillis = 0,
    ),
)

@Preview(showBackground = true, heightDp = 620)
@Composable
private fun AuthorPreview() = TierYourLifeTheme {
    AuthorScreenContent(
        state = AuthorUiState.Ready(name = "Danylo K.", photoUrl = null, lists = previewLists),
        onBack = {},
        onOpenList = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, heightDp = 620)
@Composable
private fun AuthorEmptyDarkPreview() = TierYourLifeTheme(true) {
    AuthorScreenContent(
        state = AuthorUiState.Ready(name = "Olena", photoUrl = null, lists = emptyList()),
        onBack = {},
        onOpenList = {},
        onRetry = {},
    )
}
