package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.AuthorLine
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ReportDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.ReportSentDialog
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailUiState

private val SAVE_BAR_MIN_HEIGHT = 72.dp
private val SAVE_BUTTON_MAX_WIDTH = 180.dp

@Composable
fun CommunityListScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    viewModel: CommunityListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CommunityListScreenContent(
        state = state,
        onBack = onBack,
        onMoveItem = viewModel::moveItem,
        onSave = { viewModel.saveToMyLists(onSaved) },
        onRetry = viewModel::load,
        onShow = viewModel::show,
        onAuthorClick = onAuthorClick,
        onHide = {
            viewModel.hide()
            onBack()
        },
        onHideAuthor = { uid, name ->
            viewModel.hideAuthor(uid, name)
            onBack()
        },
        onReport = viewModel::report,
        onToggleFollow = viewModel::toggleFollow,
    )
}

@Composable
internal fun CommunityListScreenContent(
    state: CommunityListUiState,
    onBack: () -> Unit,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onShow: (Showing) -> Unit = {},
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit = { _, _, _ -> },
    onHide: () -> Unit = {},
    onHideAuthor: (uid: String, name: String) -> Unit = { _, _ -> },
    onReport: (ReportReason, String?) -> Unit = { _, _ -> },
    onToggleFollow: () -> Unit = {},
) {
    var actionsVisible by remember { mutableStateOf(false) }
    var reportVisible by remember { mutableStateOf(false) }
    var reportedFrom by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when (state) {
            CommunityListUiState.Loading -> TierDetailScreenContent(
                state = TierDetailUiState.Loading,
                actions = TierDetailActions(onBack = onBack),
                readOnly = true,
            )

            CommunityListUiState.Error -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .atMost(ContentWidth.Message)
                        .testTag(CommunityTestTags.ERROR),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.community_open_failed),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.action_try_again)) }
                }
            }

            is CommunityListUiState.Success -> Column(
                Modifier
                    .fillMaxSize()
                    .testTag(CommunityTestTags.SCREEN),
            ) {
                Box(Modifier.weight(1f)) {
                    TierDetailScreenContent(
                        // Read-only while their arrangement is on screen.
                        state = TierDetailUiState.Success(state.shown),
                        actions = TierDetailActions(onBack = onBack, onMoveItem = onMoveItem),
                        // Theirs is theirs; yours is yours to arrange, and what
                        // you save is whichever is on screen.
                        readOnly = state.showing == Showing.Theirs,
                        onReaderMoreClick = { actionsVisible = true },
                        belowTopBar = {
                            AuthorLine(
                                name = state.authorName,
                                photoUrl = state.authorPhotoUrl,
                                follow = state.follow,
                                onOpenAuthor = {
                                    onAuthorClick(state.authorUid, state.authorName, state.authorPhotoUrl)
                                },
                                onToggleFollow = onToggleFollow,
                            )
                        },
                    )
                }
                // Only where there is something to switch to: a snapshot without
                // the author's arrangement has one half.
                if (state.knowsTheirs) {
                    WhoseArrangement(showing = state.showing, onShow = onShow)
                }
                SaveBar(arranged = state.arranged, saving = state.saving, onSave = onSave)
            }
        }

        val loaded = state as? CommunityListUiState.Success
        if (loaded != null) {
            if (actionsVisible) {
                ListActionsSheet(
                    title = loaded.list.title,
                    authorName = loaded.authorName,
                    authorPhotoUrl = loaded.authorPhotoUrl,
                    onDismiss = { actionsVisible = false },
                    onOpenAuthor = {
                        actionsVisible = false
                        onAuthorClick(loaded.authorUid, loaded.authorName, loaded.authorPhotoUrl)
                    },
                    onHide = {
                        actionsVisible = false
                        onHide()
                    },
                    onReport = {
                        actionsVisible = false
                        reportVisible = true
                    },
                )
            }

            if (reportVisible) {
                ReportDialog(
                    onDismiss = { reportVisible = false },
                    onSend = { reason, note ->
                        reportVisible = false
                        onReport(reason, note)
                        reportedFrom = true
                    },
                )
            }

            // Backing out only once the reader has read it: the list is already gone from their feed.
            if (reportedFrom) {
                ReportSentDialog(
                    authorName = loaded.authorName,
                    onDismiss = {
                        reportedFrom = false
                        onBack()
                    },
                    onHideAuthor = {
                        reportedFrom = false
                        onHideAuthor(loaded.authorUid, loaded.authorName)
                    },
                )
            }
        }
    }
}

/** Two boards, not one: a reader who glances at the author's finds their own work where they left it. */
@Composable
private fun WhoseArrangement(showing: Showing, onShow: (Showing) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(40.dp),
    ) {
        Showing.entries.forEachIndexed { index, which ->
            SegmentedButton(
                selected = showing == which,
                onClick = { onShow(which) },
                shape = SegmentedButtonDefaults.itemShape(index, Showing.entries.size),
                modifier = Modifier.testTag(CommunityTestTags.showing(which)),
            ) {
                Text(
                    stringResource(
                        if (which == Showing.Theirs) {
                            R.string.community_showing_theirs
                        } else {
                            R.string.community_showing_mine
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun SaveBar(arranged: Boolean, saving: Boolean, onSave: () -> Unit) {
    // Two lines each and the bar grows: "Save to my lists" is half again as long in German.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .navigationBarsPadding()
            .heightIn(min = SAVE_BAR_MIN_HEIGHT)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The tint spans the window, the sentence and the button it explains
        // do not: apart they read as two unrelated things.
        Row(
            modifier = Modifier.atMost(ContentWidth.Reading),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (arranged) R.string.community_not_saved_yet else R.string.community_someone_elses,
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag(CommunityTestTags.STATUS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier
                    .widthIn(max = SAVE_BUTTON_MAX_WIDTH)
                    .testTag(CommunityTestTags.SAVE),
            ) {
                Text(
                    text = stringResource(R.string.community_save_to_my_lists),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

private val previewList = TierList(
    id = 0,
    title = "Every A24 film",
    tiers = listOf(
        Tier(id = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList()),
        Tier(
            id = -1,
            label = "Unranked",
            colorLight = "#DAD7E0",
            colorDark = "#46464F",
            isPool = true,
            items = listOf(TierItem(1, "Hereditary", null), TierItem(2, "Moonlight", null)),
        ),
    ),
    authorName = "Olena M.",
)

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_9")
@Composable
private fun CommunityListPreview() = TierYourLifeTheme {
    CommunityListScreenContent(
        state = CommunityListUiState.Success(list = previewList, authorName = "Olena M."),
        onBack = {},
        onMoveItem = { _, _, _ -> },
        onSave = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_9")
@Composable
private fun CommunityListArrangedDarkPreview() = TierYourLifeTheme(true) {
    CommunityListScreenContent(
        state = CommunityListUiState.Success(list = previewList, authorName = "Olena M.", arranged = true),
        onBack = {},
        onMoveItem = { _, _, _ -> },
        onSave = {},
        onRetry = {},
    )
}
