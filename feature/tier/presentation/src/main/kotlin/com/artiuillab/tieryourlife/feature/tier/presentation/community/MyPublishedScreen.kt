package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.CenteredContent
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CoverIcon

@Composable
fun MyPublishedScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: MyPublishedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MyPublishedScreenContent(
        state = state,
        onBack = onBack,
        onOpen = onOpen,
        onTakeDown = viewModel::takeDown,
        onUpdate = viewModel::update,
        onRetry = viewModel::load,
    )
}

@Composable
internal fun MyPublishedScreenContent(
    state: MyPublishedUiState,
    onBack: () -> Unit,
    onOpen: (String) -> Unit = {},
    onTakeDown: (String) -> Unit = {},
    onUpdate: (String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().testTag(MyPublishedTestTags.SCREEN)) {
            TopBar(onBack)

            when (state) {
                MyPublishedUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                MyPublishedUiState.Failed -> Message(
                    title = stringResource(R.string.my_published_failed),
                    body = stringResource(R.string.home_community_failed_body),
                    action = stringResource(R.string.action_try_again),
                    onAction = onRetry,
                )

                is MyPublishedUiState.Ready -> if (state.lists.isEmpty()) {
                    Message(
                        title = stringResource(R.string.my_published_empty_title),
                        body = stringResource(R.string.my_published_empty_body),
                        action = null,
                        onAction = {},
                    )
                } else {
                    CenteredContent(ContentWidth.Reading) {
                        Text(
                            text = stringResource(R.string.my_published_subtitle),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.lists, key = { it.id }) { summary ->
                                PublishedRow(
                                    summary = summary,
                                    busy = state.removing != null || state.updating != null,
                                    canUpdate = summary.id in state.canUpdate,
                                    behind = summary.id in state.behind,
                                    onOpen = { onOpen(summary.id) },
                                    onUpdate = { onUpdate(summary.id) },
                                    onTakeDown = { onTakeDown(summary.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
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
                .testTag(MyPublishedTestTags.BACK),
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.my_published_title),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PublishedRow(
    summary: PublishedListSummary,
    busy: Boolean,
    canUpdate: Boolean,
    behind: Boolean,
    onOpen: () -> Unit,
    onUpdate: () -> Unit,
    onTakeDown: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .testTag(MyPublishedTestTags.row(summary.id))
            .padding(16.dp),
    ) {
        Text(
            text = summary.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(R.plurals.community_item_count, summary.itemCount, summary.itemCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // What the feed is showing is not what the board says any more. Said
        // here rather than left to be discovered by opening it, because the
        // person who edited the board is the last to notice: their own copy
        // looks right to them.
        if (behind) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.my_published_behind),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = onOpen,
                enabled = !busy,
                modifier = Modifier.testTag(MyPublishedTestTags.open(summary.id)),
            ) { Text(stringResource(R.string.my_published_action_open)) }
            // Offered whenever there is a board here to send. Saying "behind"
            // needs proof; offering to send again does not.
            if (canUpdate) {
                TextButton(
                    onClick = onUpdate,
                    enabled = !busy,
                    modifier = Modifier.testTag(MyPublishedTestTags.update(summary.id)),
                ) { Text(stringResource(R.string.my_published_action_update)) }
            }
            TextButton(
                onClick = onTakeDown,
                enabled = !busy,
                modifier = Modifier.testTag(MyPublishedTestTags.takeDown(summary.id)),
            ) {
                Text(
                    text = stringResource(R.string.moderation_action_take_down),
                    color = if (busy) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun Message(title: String, body: String, action: String?, onAction: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().testTag(MyPublishedTestTags.MESSAGE),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp).padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) { CoverIcon(28.dp, MaterialTheme.colorScheme.outline) }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
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
}

internal object MyPublishedTestTags {
    const val SCREEN = "my_published_screen"
    const val BACK = "my_published_back"
    const val MESSAGE = "my_published_message"

    fun row(id: String): String = "my_published_row_$id"

    fun open(id: String): String = "my_published_open_$id"

    fun update(id: String): String = "my_published_update_$id"

    fun takeDown(id: String): String = "my_published_take_down_$id"
}

@TierYourLifeDevicePreviews
@Composable
private fun MyPublishedPreview() = TierYourLifeTheme {
    MyPublishedScreenContent(
        state = MyPublishedUiState.Ready(
            listOf(
                PublishedListSummary(
                    id = "1",
                    title = "Every A24 film, ranked",
                    authorUid = "me",
                    authorName = "Olena",
                    category = ListCategory.FilmTv,
                    itemCount = 24,
                    updatedAtMillis = 0,
                ),
                PublishedListSummary(
                    id = "2",
                    title = "Ramen places in Kyiv",
                    authorUid = "me",
                    authorName = "Olena",
                    category = ListCategory.Food,
                    itemCount = 9,
                    updatedAtMillis = 0,
                ),
            ),
        ),
        onBack = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun MyPublishedEmptyPreview() = TierYourLifeTheme {
    MyPublishedScreenContent(state = MyPublishedUiState.Ready(emptyList()), onBack = {})
}
