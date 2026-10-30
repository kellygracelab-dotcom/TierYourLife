package com.artiuillab.tieryourlife.feature.community.presentation.list

import android.content.res.Resources
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AnonymousAuthorLine
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorLine
import com.artiuillab.tieryourlife.feature.community.presentation.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportDialog
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportSentDialog
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.play.BoardPlay
import com.artiuillab.tieryourlife.feature.tier.board.play.NoPlay
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

private val SWITCH_HEIGHT = 36.dp
private val SAVE_BUTTON_MAX_WIDTH = 180.dp
private const val PHASE_FADE_MILLIS = 220

@Composable
fun CommunityListScreen(
    onBack: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    onOpenList: (String) -> Unit = {},
    play: BoardPlay = NoPlay,
    onSignIn: () -> Unit = {},
    onOpenMyPublished: () -> Unit = {},
    viewModel: CommunityListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var playOpen by rememberSaveable { mutableStateOf(false) }
    // Their own list is played on their own board, not here; blank cards are not played at all.
    val playRequest = (state as? CommunityListUiState.Success)?.takeUnless { it.own || it.arriving }?.let { loaded ->
        remember(loaded.shown, loaded.mine) { PlayRequest.someoneElses(viewModel.publishedId, shown = loaded.shown, mine = loaded.mine) }
    }

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
        onOpenList = onOpenList,
        onOpenRankings = viewModel.rankings::load,
        onChooseRanking = viewModel.rankings::choose,
        onLoadMoreRankings = viewModel.rankings::loadMore,
        onWithdrawRanking = viewModel.rankings::withdrawMine,
        onPlay = { playOpen = true },
        playSaved = playRequest?.let { play.isSaved(it.key) } == true,
        onShowMyRanking = viewModel::showMyRanking,
        onSignIn = onSignIn,
        userMessages = viewModel.userMessages,
        rankingNotices = viewModel.rankingNotices,
        own = OwnListCallbacks(
            onRestore = viewModel.ownList::restore,
            onGetBack = viewModel.ownList::getBack,
            onTakeDown = viewModel.ownList::takeDown,
            onOpenMyPublished = onOpenMyPublished,
            onPublishAnonymously = viewModel.ownList::publishAnonymously,
        ),
        onUndoStartFromTheirs = viewModel::undoStartFromTheirs,
        listNotices = viewModel.listNotices,
    )
    if (playRequest != null) {
        play.Layer(request = playRequest, open = playOpen, onArranged = viewModel::putInMine, onClose = { playOpen = false })
    }
}

@Composable
fun CommunityListScreenContent(
    state: CommunityListUiState,
    onBack: () -> Unit,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onSave: (Showing) -> Unit,
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
    onOpenList: (String) -> Unit = {},
    onOpenRankings: () -> Unit = {},
    onChooseRanking: (Ranking?) -> Unit = {},
    onLoadMoreRankings: () -> Unit = {},
    onWithdrawRanking: () -> Unit = {},
    /** Play from either half; its result fills Mine. */
    onPlay: (() -> Unit)? = null,
    playSaved: Boolean = false,
    onShowMyRanking: () -> Unit = {},
    onSignIn: () -> Unit = {},
    userMessages: Flow<UserMessage> = emptyFlow(),
    rankingNotices: Flow<RankingNotice> = emptyFlow(),
    own: OwnListCallbacks = OwnListCallbacks(),
    onUndoStartFromTheirs: () -> Unit = {},
    listNotices: Flow<ListNotice> = emptyFlow(),
) {
    var actionsVisible by remember { mutableStateOf(false) }
    // A card in the air on Theirs: the strip says what dropping it will do.
    var lifted by remember { mutableStateOf(false) }
    val snackbars = remember { SnackbarHostState() }
    val loadedNow = state as? CommunityListUiState.Success
    val copyLink = rememberCopyLink(loadedNow?.listId.orEmpty(), snackbars)
    val onOwnBoard: () -> Unit = {
        when (loadedNow?.ownBoardAction) {
            OwnBoardAction.Open -> loadedNow.ownBoardId?.let(onOpenSaved)
            OwnBoardAction.Restore -> own.onRestore()
            OwnBoardAction.GetBack -> own.onGetBack()
            null -> Unit
        }
    }
    var reportVisible by remember { mutableStateOf(false) }
    var reportedFrom by remember { mutableStateOf(false) }
    var saveChoiceVisible by remember { mutableStateOf(false) }
    var rankingsVisible by remember { mutableStateOf(false) }
    val openRankings = {
        onOpenRankings()
        rankingsVisible = true
    }
    val onSaveClick: () -> Unit = { saveTapped(loadedNow, askWhich = { saveChoiceVisible = true }, save = onSave) }

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
                            actions = TierDetailActions(
                                onBack = onBack,
                                onMoveItem = onMoveItem,
                                onPlay = onPlay.takeUnless { loaded.own || loaded.arriving },
                                playSaved = playSaved,
                                onCardLifted = { lifted = it },
                                poolSide = loaded.showing,
                                emptyPoolCounted = false,
                            ),
                            // Theirs is theirs, and their own list changes on its board; a move
                            // on Theirs still arrives, and starts the reader's own version from it.
                            readOnly = loaded.readOnly,
                            onReaderMoreClick = { actionsVisible = true },
                            belowTopBar = {
                                UnderTheTitle(
                                    loaded = loaded,
                                    onAuthorClick = onAuthorClick,
                                    onToggleFollow = onToggleFollow,
                                    onOpenList = onOpenList,
                                )
                            },
                            // In the pool sheet's strip, in reach whether the pool is folded or open.
                            poolAccessories = { _ ->
                                ReaderStrip(
                                    state = loaded,
                                    lifted = lifted,
                                    onShow = onShow,
                                    onSaveClick = onSaveClick,
                                    onOpenSaved = onOpenSaved,
                                    onOwnBoard = onOwnBoard,
                                    onCopyLink = copyLink,
                                )
                            },
                            // The strip's sentence, under the side pool's title.
                            poolNote = { ReaderStatusLine(loaded, lifted) },
                            // The same, in the bar, when the pool sits beside the rows.
                            barExtras = { mode ->
                                ReaderBarControls(
                                    state = loaded,
                                    mode = mode,
                                    onShow = onShow,
                                    onSaveClick = onSaveClick,
                                    onOpenSaved = onOpenSaved,
                                    onOpenAuthor = { onAuthorClick(loaded.authorUid, loaded.authorName, loaded.authorPhotoUrl) },
                                    onOwnBoard = onOwnBoard,
                                    onCopyLink = copyLink,
                                )
                            },
                            // On a tablet held upright the bar grows and says who and where under the title.
                            titleSubline = {
                                AuthorAndSection(
                                    state = loaded,
                                    onOpenAuthor = { onAuthorClick(loaded.authorUid, loaded.authorName, loaded.authorPhotoUrl) },
                                )
                            },
                        )
                    }
                }
            }
        }

        val loaded = state as? CommunityListUiState.Success
        if (loaded != null) {
            if (rankingsVisible) {
                RankingsSheet(
                    state = loaded,
                    onDismiss = { rankingsVisible = false },
                    onChoose = { ranking ->
                        rankingsVisible = false
                        onChooseRanking(ranking)
                    },
                    onLoadMore = onLoadMoreRankings,
                    onWithdraw = onWithdrawRanking,
                )
            }

            if (saveChoiceVisible) {
                SaveChoiceDialog(
                    arranged = loaded.arranged,
                    chosenName = loaded.chosenRanking?.authorName,
                    onDismiss = { saveChoiceVisible = false },
                    onChoose = { which ->
                        saveChoiceVisible = false
                        onSave(which)
                    },
                )
            }

            if (actionsVisible) {
                ListMenu(
                    loaded = loaded,
                    onDismiss = { actionsVisible = false },
                    onOpenAuthor = { onAuthorClick(loaded.authorUid, loaded.authorName, loaded.authorPhotoUrl) },
                    onHide = onHide,
                    onReport = { reportVisible = true },
                    onShowMyRanking = { if (loaded.signedIn) onShowMyRanking() else onSignIn() },
                    onOpenRankings = openRankings,
                    own = own,
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
        ListSnackbar(
            host = snackbars,
            userMessages = userMessages,
            rankingNotices = rankingNotices,
            listNotices = listNotices,
            onSignIn = onSignIn,
            onUndoStartFromTheirs = onUndoStartFromTheirs,
            onTakenDown = onBack,
        )
    }
}

/** Two arrangements to choose from only once the author's is known; nothing to save while the cards are blank. */
private fun saveTapped(state: CommunityListUiState.Success?, askWhich: () -> Unit, save: (Showing) -> Unit) {
    when {
        state == null || state.arriving -> Unit
        state.knowsTheirs -> askWhich()
        else -> save(Showing.Mine)
    }
}

/**
 * The list's menu: the readers' rankings first, on any list. Then its author,
 * Hide and Report on somebody else's list, and once the reader has arranged
 * their own half, showing it to others. On their own list: their public
 * lists, the name on or off, and taking it down. Each closes the menu first.
 */
@Composable
private fun ListMenu(
    loaded: CommunityListUiState.Success,
    onDismiss: () -> Unit,
    onOpenAuthor: () -> Unit,
    onHide: () -> Unit,
    onReport: () -> Unit,
    onShowMyRanking: () -> Unit,
    onOpenRankings: () -> Unit,
    own: OwnListCallbacks,
) {
    fun closing(action: () -> Unit): () -> Unit = {
        onDismiss()
        action()
    }
    val mine = loaded.own
    ListActionsSheet(
        title = loaded.list.title,
        authorName = loaded.authorName.takeUnless { loaded.editorial },
        authorPhotoUrl = loaded.authorPhotoUrl,
        onDismiss = onDismiss,
        onOpenAuthor = closing(onOpenAuthor).takeUnless { loaded.editorial || mine },
        onHide = closing(onHide).takeUnless { mine },
        onReport = closing(onReport).takeUnless { mine },
        onShowMyRanking = closing(onShowMyRanking).takeIf { loaded.canShowRanking },
        signInToShowRanking = !loaded.signedIn,
        onOpenMyPublished = closing(own.onOpenMyPublished).takeIf { mine },
        // Sent again from the board, so only where the board is on this phone.
        onPublishAnonymously = closing { own.onPublishAnonymously(!loaded.anonymous) }.takeIf { mine && loaded.ownBoardId != null },
        anonymous = loaded.anonymous,
        onTakeDown = closing(own.onTakeDown).takeIf { mine },
        onOpenRankings = closing(onOpenRankings),
        rankingCount = loaded.rankingCount,
    )
}

/**
 * What went wrong, what became of a ranking the reader asked to show, and a
 * version started from Theirs, with Undo. A guest can go and sign in from it;
 * their own list taken down leaves the page.
 */
@Composable
private fun ListSnackbar(
    host: SnackbarHostState,
    userMessages: Flow<UserMessage>,
    rankingNotices: Flow<RankingNotice>,
    listNotices: Flow<ListNotice>,
    onSignIn: () -> Unit,
    onUndoStartFromTheirs: () -> Unit,
    onTakenDown: () -> Unit,
) {
    val resources = LocalResources.current
    val signIn by rememberUpdatedState(onSignIn)
    val undo by rememberUpdatedState(onUndoStartFromTheirs)
    val leave by rememberUpdatedState(onTakenDown)
    LaunchedEffect(userMessages) { userMessages.collect { host.showSnackbar(resources.getString(ThemeR.string.snack_action_failed)) } }
    LaunchedEffect(rankingNotices) {
        rankingNotices.collect { notice ->
            when (notice) {
                RankingNotice.Shown -> host.showSnackbar(resources.getString(R.string.community_ranking_shown))
                RankingNotice.SignInFirst -> {
                    val answer = host.showSnackbar(
                        message = resources.getString(R.string.community_sign_in_to_show_ranking),
                        actionLabel = resources.getString(R.string.community_action_sign_in),
                    )
                    if (answer == SnackbarResult.ActionPerformed) signIn()
                }
            }
        }
    }
    LaunchedEffect(listNotices) {
        listNotices.collect { notice ->
            when (notice) {
                ListNotice.TakenDown -> leave()
                is ListNotice.StartedFromTheirs -> {
                    host.currentSnackbarData?.dismiss()
                    val answer = host.showSnackbar(
                        message = resources.startedFromTheirs(notice),
                        actionLabel = resources.getString(R.string.community_action_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (answer == SnackbarResult.ActionPerformed) undo()
                }
            }
        }
    }
    SnackbarHost(host, Modifier.fillMaxSize().wrapContentHeight(Alignment.Bottom).navigationBarsPadding())
}

private fun Resources.startedFromTheirs(notice: ListNotice.StartedFromTheirs): String {
    val name = notice.name
    return when {
        notice.replaced && name != null -> getString(R.string.community_replaced_yours, name)
        notice.replaced -> getString(R.string.community_replaced_yours_unnamed)
        name != null -> getString(R.string.community_started_from_theirs, name)
        else -> getString(R.string.community_started_from_theirs_unnamed)
    }
}

/** Under the title: the author and the list this one was changed from. */
@Composable
private fun UnderTheTitle(
    loaded: CommunityListUiState.Success,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    onToggleFollow: () -> Unit,
    onOpenList: (String) -> Unit,
) {
    // One of the app's own lists has no line to pin here; somebody's anonymous one has a line that leads nowhere.
    if (loaded.own) {
        OwnListLine(loaded)
    } else if (!loaded.editorial) {
        AuthorLine(
            name = loaded.authorName,
            photoUrl = loaded.authorPhotoUrl,
            follow = loaded.follow,
            onOpenAuthor = { onAuthorClick(loaded.authorUid, loaded.authorName, loaded.authorPhotoUrl) },
            onToggleFollow = onToggleFollow,
        )
    } else if (loaded.byNobody) {
        AnonymousAuthorLine()
    }
    val basedOn = loaded.basedOn
    if (basedOn != null) {
        BasedOnLine(basedOn = basedOn, onOpen = { onOpenList(basedOn.id) })
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
    lifted: Boolean,
    onShow: (Showing) -> Unit,
    onSaveClick: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onOwnBoard: () -> Unit,
    onCopyLink: () -> Unit,
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
            // the author's arrangement has one half, and their own list has only theirs.
            if (state.knowsTheirs && !state.own) {
                WhoseArrangement(
                    showing = state.showing,
                    theirsLabel = state.chosenRanking?.authorName ?: stringResource(R.string.community_showing_theirs),
                    onShow = onShow,
                )
            }
            Spacer(Modifier.weight(1f))
            val savedId = state.savedId
            if (state.own) {
                CopyLinkButton(onClick = onCopyLink)
                OwnBoardButton(state = state, onClick = onOwnBoard)
            } else if (savedId != null) {
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
                    onClick = onSaveClick,
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
        if (state.foldsToRankIt && !lifted) {
            RankItYourselfLine()
        } else {
            Text(
                text = statusText(state, lifted),
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
}

/**
 * Theirs with nothing left unranked and nothing of the reader's yet: the
 * folded pool has one thing to say, that the cards are there to be moved.
 */
internal val CommunityListUiState.Success.foldsToRankIt: Boolean
    get() = !own &&
        showing == Showing.Theirs &&
        !arranged &&
        savedId == null &&
        shown.tiers.firstOrNull { it.isPool }?.items.isNullOrEmpty()

/**
 * Asked at the moment of saving rather than read off the switch: a reader
 * who glanced at the author's half and tapped Save did not mean to throw
 * their own away, nor the other way round.
 */
@Composable
private fun SaveChoiceDialog(arranged: Boolean, chosenName: String?, onDismiss: () -> Unit, onChoose: (Showing) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(CommunityListTestTags.SAVE_CHOICE),
        title = { Text(stringResource(R.string.community_save_which_title)) },
        text = { Text(stringResource(R.string.community_save_which_body)) },
        confirmButton = {
            TextButton(
                onClick = { onChoose(Showing.Theirs) },
                modifier = Modifier.testTag(CommunityListTestTags.SAVE_THEIRS),
            ) { Text(chosenName ?: stringResource(R.string.community_save_theirs)) }
        },
        dismissButton = {
            TextButton(
                onClick = { onChoose(Showing.Mine) },
                modifier = Modifier.testTag(CommunityListTestTags.SAVE_MINE),
            ) {
                // Nothing arranged yet: what "mine" would keep is every card unranked, and that is said plainly.
                Text(stringResource(if (arranged) R.string.community_save_mine else R.string.community_save_mine_empty))
            }
        },
    )
}

/** Two boards, not one: a reader who glances at the author's finds their own work where they left it. */
@Composable
internal fun WhoseArrangement(showing: Showing, theirsLabel: String, onShow: (Showing) -> Unit, height: Dp = SWITCH_HEIGHT) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.height(height)) {
        Showing.entries.forEachIndexed { index, which ->
            SegmentedButton(
                selected = showing == which,
                onClick = { onShow(which) },
                shape = SegmentedButtonDefaults.itemShape(index, Showing.entries.size),
                modifier = Modifier.testTag(CommunityListTestTags.showing(which)),
                // The fill already says which; a tick beside two short words only crowds them.
                icon = {},
            ) {
                Text(
                    // The left half is whoever's placing is on screen: the author's, or a reader's by name.
                    text = if (which == Showing.Theirs) theirsLabel else stringResource(R.string.community_showing_mine),
                    modifier = Modifier.widthIn(max = NAME_MAX_WIDTH),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val NAME_MAX_WIDTH = 120.dp

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

/** What the reader's copy is, in the words of the strip's sentence. */
private val CommunityListUiState.Success.statusRes: Int
    get() = when {
        own -> R.string.community_own_list_line
        savedId != null -> R.string.community_saved_to_your_lists
        arranged -> R.string.community_not_saved_yet
        // "Someone else's" would name an owner a list without an author does not have.
        editorial -> R.string.community_rank_it_your_way
        else -> R.string.community_someone_elses
    }

/** The strip's sentence; while a card is in the air on Theirs, what dropping it does. */
@Composable
private fun statusText(state: CommunityListUiState.Success, lifted: Boolean): String {
    if (!lifted || state.own || state.showing != Showing.Theirs) return stringResource(state.statusRes)
    val name = state.theirsName
    return if (name != null) {
        stringResource(R.string.community_drop_to_start_yours, name)
    } else {
        stringResource(R.string.community_drop_to_start_yours_unnamed)
    }
}

/** The same sentence, small, under the side pool's title. */
@Composable
private fun ReaderStatusLine(state: CommunityListUiState.Success, lifted: Boolean) {
    Text(
        text = statusText(state, lifted),
        modifier = Modifier.testTag(CommunityListTestTags.STATUS),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}
