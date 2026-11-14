package com.artiuillab.tieryourlife.feature.community.presentation.list

import android.content.res.Resources
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.ui.RankingChoiceDialog
import com.artiuillab.tieryourlife.core.theme.ui.ReturnToPoolDialog
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorLine
import com.artiuillab.tieryourlife.feature.community.presentation.components.ListActionsSheet
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportDialog
import com.artiuillab.tieryourlife.feature.community.presentation.components.ReportSentDialog
import com.artiuillab.tieryourlife.feature.community.presentation.follow.sentenceFor
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.board.common.currentLanguageTag
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.AboutTheCover
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
    viewModel: CommunityListViewModel = hiltViewModel(),
) {
    val written by viewModel.state.collectAsStateWithLifecycle()
    val languageTag = currentLanguageTag()
    val state = remember(written, languageTag) { written.readIn(languageTag) }
    var playOpen by rememberSaveable { mutableStateOf(false) }
    // Their own list is played on their own board, not here; blank cards are not played at all.
    val playRequest = (state as? CommunityListUiState.Success)?.takeUnless { it.own || it.arriving }?.let { loaded ->
        remember(loaded.shown, loaded.mine, loaded.version) {
            PlayRequest.someoneElses(viewModel.publishedId, loaded.version, shown = loaded.shown, mine = loaded.mine)
        }
    }

    CommunityListScreenContent(
        state = state,
        onBack = onBack,
        onMoveItem = viewModel::moveItem,
        onSave = { viewModel.saveToMyLists(languageTag) },
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
        onLikeRanking = viewModel.rankings::like,
        onReturnAllToPool = viewModel.rankings::returnAllToPool,
        onPlay = { playOpen = true },
        playSaved = playRequest?.let { play.isSaved(it.key) } == true,
        onShowMyRanking = { viewModel.showMyRanking(languageTag) },
        onSignIn = onSignIn,
        userMessages = viewModel.userMessages,
        rankingNotices = viewModel.rankingNotices,
        own = OwnListCallbacks(
            onRestore = viewModel.ownList::restore,
            onGetBack = viewModel.ownList::getBack,
            onTakeDown = viewModel.ownList::takeDown,
        ),
        onUndoStartFromTheirs = viewModel::undoStartFromTheirs,
        listNotices = viewModel.listNotices,
        onTurnGame = viewModel.game::turn,
    )
    val loaded = state as? CommunityListUiState.Success
    val offer = loaded?.rankingOffer
    if (offer != null) {
        RankingChoiceDialog(
            signedIn = loaded.signedIn,
            busy = loaded.saving,
            onShow = { viewModel.choices.choose(public = true, languageTag) },
            onNotNow = viewModel.choices::dismiss,
        )
    }
    if (playRequest != null) {
        play.Layer(request = playRequest, open = playOpen, onArranged = viewModel::putInMine, onClose = { playOpen = false })
    }
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
    onOpenList: (String) -> Unit = {},
    onOpenRankings: () -> Unit = {},
    onChooseRanking: (Ranking?) -> Unit = {},
    onLoadMoreRankings: () -> Unit = {},
    onWithdrawRanking: () -> Unit = {},
    onLikeRanking: (Ranking) -> Unit = {},
    onReturnAllToPool: () -> Unit = {},
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
    /** The board before the last game, or its result again. */
    onTurnGame: () -> Unit = {},
) {
    var actionsVisible by remember { mutableStateOf(false) }
    var returnToPoolVisible by rememberSaveable { mutableStateOf(false) }
    // A card in the air on Theirs: the strip says what dropping it will do.
    var lifted by remember { mutableStateOf(false) }
    val snackbars = remember { SnackbarHostState() }
    val sheetSnackbars = remember { SnackbarHostState() }
    val loadedNow = state as? CommunityListUiState.Success
    val copyLink = rememberCopyLink(loadedNow?.listId.orEmpty(), snackbars)
    val onOwnBoard: () -> Unit = {
        when (loadedNow?.ownBoardAction) {
            OwnBoardAction.Restore -> own.onRestore()
            OwnBoardAction.GetBack -> own.onGetBack()
            null -> Unit
        }
    }
    var reportVisible by remember { mutableStateOf(false) }
    var coverVisible by remember { mutableStateOf(false) }
    var reportedFrom by remember { mutableStateOf(false) }
    var rankingsVisible by remember { mutableStateOf(false) }
    var removeRankingVisible by remember { mutableStateOf(false) }
    val openRankings = {
        onOpenRankings()
        rankingsVisible = true
    }
    val onSaveClick: () -> Unit = { if (loadedNow?.arriving == false) onSave() }

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
                                // As on the website: one's own cards go on one's own copy, after Save.
                                onAddClick = null,
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
                                    onTurnGame = onTurnGame,
                                )
                            },
                            // The strip's sentence, under the side pool's title.
                            poolNote = { ReaderStatusLine(loaded, lifted, onTurnGame) },
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
                    onWithdraw = { removeRankingVisible = true },
                    onLike = onLikeRanking,
                    snackbars = sheetSnackbars,
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
                    onReturnAllToPool = { returnToPoolVisible = true },
                    onAboutCover = { coverVisible = true },
                    onRemoveRanking = { removeRankingVisible = true },
                    own = own,
                )
            }

            RemoveRankingDialog(
                visible = removeRankingVisible,
                onDismiss = { removeRankingVisible = false },
                onRemove = {
                    removeRankingVisible = false
                    onWithdrawRanking()
                },
            )

            CoverSheet(loaded.list.title, loaded.cover.takeIf { coverVisible }, onDismiss = { coverVisible = false })

            if (returnToPoolVisible) {
                ReturnToPoolDialog(
                    onDismiss = { returnToPoolVisible = false },
                    onConfirm = {
                        returnToPoolVisible = false
                        onReturnAllToPool()
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
        ListSnackbar(
            host = snackbars,
            sheetHost = { sheetSnackbars.takeIf { rankingsVisible } },
            userMessages = userMessages,
            rankingNotices = rankingNotices,
            listNotices = listNotices,
            onSignIn = onSignIn,
            onUndoStartFromTheirs = onUndoStartFromTheirs,
            onTakenDown = onBack,
            onOpenBoard = onOpenSaved,
        )
    }
}

/** Taking one's own ranking out of the list's Rankings, asked first: the board on this phone stays. */
@Composable
private fun RemoveRankingDialog(visible: Boolean, onDismiss: () -> Unit, onRemove: () -> Unit) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(CommunityListTestTags.REMOVE_RANKING_DIALOG),
        title = { Text(stringResource(R.string.community_remove_ranking_title)) },
        text = { Text(stringResource(R.string.community_remove_ranking_body)) },
        confirmButton = {
            TextButton(onClick = onRemove, modifier = Modifier.testTag(CommunityListTestTags.REMOVE_RANKING_CONFIRM)) {
                Text(stringResource(R.string.community_remove_ranking_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(ThemeR.string.action_cancel)) } },
    )
}

/** Who made a house list's cover, while the menu's line for it is open. */
@Composable
private fun CoverSheet(title: String, cover: CoverCredit?, onDismiss: () -> Unit) {
    cover ?: return
    AdaptiveSheet(onDismiss = onDismiss, width = SheetWidth.Choosing) {
        AboutTheCover(title, cover.imageUrl, cover.attribution, onDismiss = onDismiss)
    }
}

/**
 * The list's menu: the readers' rankings first, on any list. Then its author,
 * Hide and Report on somebody else's list, and once the reader has arranged
 * their own half, showing it to others. On their own list: their public
 * lists, and taking it down unless the board it was opened from does that.
 * Each closes the menu first.
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
    onReturnAllToPool: () -> Unit,
    onAboutCover: () -> Unit,
    onRemoveRanking: () -> Unit,
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
        rankingShown = loaded.rankingShown,
        onRemoveRanking = closing(onRemoveRanking).takeIf { loaded.rankingShown && loaded.signedIn && !mine },
        // The board's own Public switch does it there, and keeps the board in step.
        onTakeDown = closing(own.onTakeDown).takeIf { mine && !loaded.fromBoard },
        onOpenRankings = closing(onOpenRankings),
        rankingCount = loaded.rankingCount,
        onReturnAllToPool = closing(onReturnAllToPool).takeIf { !loaded.own && !loaded.arriving && loaded.showing == Showing.Mine },
        canReturnAllToPool = loaded.mine.tiers.any { !it.isPool && it.items.isNotEmpty() },
        onAboutCover = closing(onAboutCover).takeIf { loaded.cover != null },
    )
}

/**
 * What went wrong, what became of a ranking the reader asked to show, and a
 * version started from Theirs, with Undo. A guest can go and sign in from it;
 * their own list taken down leaves the page, and its board found here replaces it.
 */
@Composable
private fun ListSnackbar(
    host: SnackbarHostState,
    /** The Rankings sheet's own, while it is up: the page's would be under it. */
    sheetHost: () -> SnackbarHostState?,
    userMessages: Flow<UserMessage>,
    rankingNotices: Flow<RankingNotice>,
    listNotices: Flow<ListNotice>,
    onSignIn: () -> Unit,
    onUndoStartFromTheirs: () -> Unit,
    onTakenDown: () -> Unit,
    onOpenBoard: (Long) -> Unit,
) {
    val resources = LocalResources.current
    val signIn by rememberUpdatedState(onSignIn)
    val undo by rememberUpdatedState(onUndoStartFromTheirs)
    val leave by rememberUpdatedState(onTakenDown)
    val openBoard by rememberUpdatedState(onOpenBoard)
    val onTop by rememberUpdatedState(sheetHost)
    LaunchedEffect(userMessages) {
        userMessages.collect {
            val text = if (it == UserMessage.TargetIsStaff) ThemeR.string.snack_target_is_staff else ThemeR.string.snack_action_failed
            (onTop() ?: host).showSnackbar(resources.getString(text))
        }
    }
    LaunchedEffect(rankingNotices) {
        rankingNotices.collect { notice ->
            val target = onTop() ?: host
            when (notice) {
                RankingNotice.Shown -> target.showSnackbar(resources.getString(R.string.community_ranking_shown))
                RankingNotice.ListChanged -> target.showSnackbar(resources.getString(R.string.community_ranking_list_changed))
                RankingNotice.Banned -> target.showSnackbar(resources.getString(R.string.community_ranking_banned))
                RankingNotice.OwnList -> target.showSnackbar(resources.getString(R.string.community_ranking_own_list))
                RankingNotice.ListGone -> target.showSnackbar(resources.getString(R.string.community_ranking_list_gone))
                RankingNotice.BadTierNames -> target.showSnackbar(resources.getString(R.string.community_ranking_bad_tier_names))
                RankingNotice.CardsDontMatch -> target.showSnackbar(resources.getString(R.string.community_ranking_cards_dont_match))
                RankingNotice.NotAccepted -> target.showSnackbar(resources.getString(R.string.community_ranking_not_accepted))
                RankingNotice.TooManyWebPictures -> target.showSnackbar(resources.getString(ThemeR.string.refusal_too_many_web_pictures))
                RankingNotice.WebPicturesToday -> target.showSnackbar(resources.getString(ThemeR.string.refusal_web_pictures_today))
                RankingNotice.SignInFirst -> {
                    val answer = target.showSnackbar(
                        message = resources.getString(R.string.community_sign_in_to_show_ranking),
                        actionLabel = resources.getString(R.string.community_action_sign_in),
                    )
                    if (answer == SnackbarResult.ActionPerformed) signIn()
                }
                RankingNotice.SignInToLike -> {
                    val answer = target.showSnackbar(
                        message = resources.getString(R.string.community_sign_in_to_like),
                        actionLabel = resources.getString(R.string.community_action_sign_in),
                    )
                    if (answer == SnackbarResult.ActionPerformed) signIn()
                }
                RankingNotice.TooManyLikes -> target.showSnackbar(resources.getString(R.string.community_too_many_likes))
            }
        }
    }
    LaunchedEffect(listNotices) {
        listNotices.collect { notice ->
            when (notice) {
                ListNotice.TakenDown -> leave()
                is ListNotice.OpenBoard -> openBoard(notice.boardId)
                ListNotice.SignInToFollow -> signIn()
                is ListNotice.FollowRefused -> host.showSnackbar(resources.sentenceFor(notice.refusal))
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
    } else if (loaded.namedAuthor != null) {
        AuthorLine(
            name = loaded.authorName,
            photoUrl = loaded.authorPhotoUrl,
            follow = loaded.follow,
            onOpenAuthor = { onAuthorClick(loaded.authorUid, loaded.authorName, loaded.authorPhotoUrl) },
            onToggleFollow = onToggleFollow,
        )
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
    onTurnGame: () -> Unit,
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
            // Only where there is something to switch to: a template or a snapshot
            // without the author's arrangement has one half, and their own list has only theirs.
            if (state.twoHalves) {
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
                    Text(text = stringResource(R.string.community_saved), textAlign = TextAlign.Center)
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
                        text = stringResource(state.saveLabel),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
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
            GameStepButton(state, onTurnGame)
        }
    }
}

/**
 * Undo after a game, and Redo after that: the board before the game and its
 * result, for as long as nothing else changes on the board. It stays on the
 * page, so an offer opening over the board takes nothing away.
 */
@Composable
private fun GameStepButton(state: CommunityListUiState.Success, onTurnGame: () -> Unit) {
    val step = state.gameStep?.takeUnless { state.own } ?: return
    TextButton(
        onClick = onTurnGame,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier
            .height(GAME_STEP_HEIGHT)
            .testTag(CommunityListTestTags.GAME_STEP),
    ) {
        Text(stringResource(if (step == GameStep.Undo) R.string.community_action_undo else R.string.community_action_redo), maxLines = 1)
    }
}

private val GAME_STEP_HEIGHT = 32.dp

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

/** The same sentence, small, under the side pool's title, and a game's Undo under it. */
@Composable
private fun ReaderStatusLine(state: CommunityListUiState.Success, lifted: Boolean, onTurnGame: () -> Unit) {
    Column {
        Text(
            text = statusText(state, lifted),
            modifier = Modifier.testTag(CommunityListTestTags.STATUS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        GameStepButton(state, onTurnGame)
    }
}
