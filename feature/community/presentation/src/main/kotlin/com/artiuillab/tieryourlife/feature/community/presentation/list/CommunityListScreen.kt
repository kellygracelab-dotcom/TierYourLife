package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorLine
import com.artiuillab.tieryourlife.feature.community.presentation.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportDialog
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportSentDialog
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private val SWITCH_HEIGHT = 36.dp
private val SAVE_BUTTON_MAX_WIDTH = 180.dp
private const val PHASE_FADE_MILLIS = 150

@Composable
fun CommunityListScreen(
    onBack: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    viewModel: CommunityListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CommunityListScreenContent(
        state = state,
        onBack = onBack,
        onMoveItem = viewModel::moveItem,
        onSave = viewModel::saveToMyLists,
        onRetry = viewModel::load,
        preview = viewModel.preview,
        onOpenSaved = onOpenSaved,
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
fun CommunityListScreenContent(
    state: CommunityListUiState,
    onBack: () -> Unit,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    /** What the card that opened this list showed; null when it was opened by id alone. */
    preview: ListPreview? = null,
    onOpenSaved: (Long) -> Unit = {},
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
        // The placeholder, the board and the failure trade places with a short
        // fade, never with a blank frame between them.
        Crossfade(targetState = state.phase, animationSpec = tween(PHASE_FADE_MILLIS), label = "community list") { phase ->
            when (phase) {
                Phase.Loading -> if (preview != null) {
                    ListSkeleton(preview = preview, onBack = onBack)
                } else {
                    TierDetailScreenContent(
                        state = TierDetailUiState.Loading,
                        actions = TierDetailActions(onBack = onBack),
                        readOnly = true,
                    )
                }

                Phase.Error -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier
                            .atMost(ContentWidth.Message)
                            .testTag(CommunityListTestTags.ERROR),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.community_open_failed),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = onRetry) { Text(stringResource(ThemeR.string.action_try_again)) }
                    }
                }

                Phase.Ready -> {
                    // While the fade runs the state may already be another phase's.
                    val loaded = state as? CommunityListUiState.Success ?: return@Crossfade
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(CommunityListTestTags.SCREEN),
                    ) {
                        TierDetailScreenContent(
                            // Read-only while their arrangement is on screen.
                            state = TierDetailUiState.Success(loaded.shown),
                            actions = TierDetailActions(onBack = onBack, onMoveItem = onMoveItem),
                            // Theirs is theirs; yours is yours to arrange, and what
                            // you save is whichever is on screen.
                            readOnly = loaded.showing == Showing.Theirs,
                            onReaderMoreClick = { actionsVisible = true },
                            belowTopBar = {
                                // A list that stands without an author has no line to pin here.
                                if (!loaded.editorial) {
                                    AuthorLine(
                                        name = loaded.authorName,
                                        photoUrl = loaded.authorPhotoUrl,
                                        follow = loaded.follow,
                                        onOpenAuthor = {
                                            onAuthorClick(loaded.authorUid, loaded.authorName, loaded.authorPhotoUrl)
                                        },
                                        onToggleFollow = onToggleFollow,
                                    )
                                }
                            },
                            // In the pool sheet's strip, in reach whether the pool is folded or open.
                            poolAccessories = { _ ->
                                ReaderStrip(state = loaded, onShow = onShow, onSave = onSave, onOpenSaved = onOpenSaved)
                            },
                        )
                    }
                }
            }
        }

        val loaded = state as? CommunityListUiState.Success
        if (loaded != null) {
            if (actionsVisible) {
                ListActionsSheet(
                    title = loaded.list.title,
                    authorName = loaded.authorName.takeUnless { loaded.editorial },
                    authorPhotoUrl = loaded.authorPhotoUrl,
                    onDismiss = { actionsVisible = false },
                    onOpenAuthor = if (loaded.editorial) {
                        null
                    } else {
                        {
                            actionsVisible = false
                            onAuthorClick(loaded.authorUid, loaded.authorName, loaded.authorPhotoUrl)
                        }
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
                    authorName = loaded.authorName.takeUnless { loaded.editorial },
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

/**
 * What the reader does with the board, in the pool sheet's strip: whose
 * arrangement is on screen, the save (or the copy once there is one), and
 * under them the sentence that explains the button.
 */
@Composable
private fun ReaderStrip(
    state: CommunityListUiState.Success,
    onShow: (Showing) -> Unit,
    onSave: () -> Unit,
    onOpenSaved: (Long) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Only where there is something to switch to: a snapshot without
            // the author's arrangement has one half.
            if (state.knowsTheirs) {
                WhoseArrangement(showing = state.showing, onShow = onShow)
            }
            Spacer(Modifier.weight(1f))
            val savedId = state.savedId
            if (savedId != null) {
                Button(
                    onClick = { onOpenSaved(savedId) },
                    modifier = Modifier
                        .widthIn(max = SAVE_BUTTON_MAX_WIDTH)
                        .testTag(CommunityListTestTags.OPEN_SAVED),
                ) {
                    Text(text = stringResource(R.string.community_action_open), textAlign = TextAlign.Center)
                }
            } else {
                Button(
                    onClick = onSave,
                    enabled = !state.saving,
                    modifier = Modifier
                        .widthIn(max = SAVE_BUTTON_MAX_WIDTH)
                        .testTag(CommunityListTestTags.SAVE),
                ) {
                    Text(
                        text = stringResource(R.string.community_save_to_my_lists),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
        Text(
            text = stringResource(
                when {
                    state.savedId != null -> R.string.community_saved_to_your_lists
                    state.arranged -> R.string.community_not_saved_yet
                    // "Someone else's" would name an owner a list without an author does not have.
                    state.editorial -> R.string.community_rank_it_your_way
                    else -> R.string.community_someone_elses
                },
            ),
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag(CommunityListTestTags.STATUS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Two boards, not one: a reader who glances at the author's finds their own work where they left it. */
@Composable
private fun WhoseArrangement(showing: Showing, onShow: (Showing) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.height(SWITCH_HEIGHT)) {
        Showing.entries.forEachIndexed { index, which ->
            SegmentedButton(
                selected = showing == which,
                onClick = { onShow(which) },
                shape = SegmentedButtonDefaults.itemShape(index, Showing.entries.size),
                modifier = Modifier.testTag(CommunityListTestTags.showing(which)),
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

private enum class Phase { Loading, Ready, Error }

private val CommunityListUiState.phase: Phase
    get() = when (this) {
        CommunityListUiState.Loading -> Phase.Loading
        is CommunityListUiState.Success -> Phase.Ready
        CommunityListUiState.Error -> Phase.Error
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
