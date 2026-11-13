package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.layout.FullScreenTask
import com.artiuillab.tieryourlife.core.theme.layout.isShortWindow
import com.artiuillab.tieryourlife.core.theme.ui.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.feature.tier.board.play.BoardPlay
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.launch
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** Play as navigation hands it to the board screens. Its view model belongs to the screen beneath. */
object PlayOverBoards : BoardPlay {
    @Composable
    override fun isSaved(key: String): Boolean {
        val viewModel: PlayViewModel = hiltViewModel()
        LaunchedEffect(viewModel, key) { viewModel.watch(key) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        return key in state.saved
    }

    @Composable
    override fun Layer(request: PlayRequest, open: Boolean, onArranged: (board: TierList, undone: Boolean) -> Unit, onClose: () -> Unit) {
        PlayLayer(request, open, onArranged, onClose, hiltViewModel())
    }
}

@Composable
internal fun PlayLayer(
    request: PlayRequest,
    open: Boolean,
    onArranged: (board: TierList, undone: Boolean) -> Unit,
    onClose: () -> Unit,
    viewModel: PlayViewModel,
) {
    LaunchedEffect(viewModel, request) { viewModel.offer(request) }
    LaunchedEffect(viewModel, open) { if (open) viewModel.open() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val arranged by rememberUpdatedState(onArranged)
    val close by rememberUpdatedState(onClose)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PlayEvent.Closed -> close()
                is PlayEvent.Arranged -> arranged(event.board, event.undone)
                is PlayEvent.Said -> launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val (message, action) = noticeText(event.notice, resources)
                    val answer = snackbar.showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Short)
                    if (answer == SnackbarResult.ActionPerformed) act(event.notice, viewModel)
                }
            }
        }
    }

    val stage = state.stage
    val picker = pickerOver(stage)
    Box(Modifier.fillMaxSize()) {
        when (stage) {
            PlayStage.Closed, is PlayStage.Picking -> Unit
            is PlayStage.Dueling -> {
                FullScreenTask()
                BackHandler(onBack = viewModel.duel::quit)
                DuelScreen(
                    duel = stage.duel,
                    onPick = viewModel.duel::pick,
                    onSkip = viewModel.duel::skip,
                    onUndo = viewModel.duel::undo,
                    onQuit = viewModel.duel::quit,
                )
            }
            is PlayStage.Finished -> {
                FullScreenTask()
                BackHandler(onBack = viewModel.duel::quit)
                ResultScreen(
                    result = stage.result,
                    actions = ResultActions(
                        onApply = viewModel.result::apply,
                        onRefine = viewModel.result::refine,
                        onPlayAgain = viewModel.result::playAgain,
                        onCancel = viewModel.result::cancel,
                        onQuit = viewModel.duel::quit,
                        onUnplayed = viewModel.result::unplayed,
                    ),
                )
            }
        }
        if (picker != null) {
            PickerSheet(
                picker = picker,
                actions = PickerActions(
                    onChoose = viewModel.picker::choose,
                    onShowCards = viewModel.picker::showCards,
                    onShowGames = viewModel.picker::showGames,
                    onShowIndividual = viewModel.picker::showIndividual,
                    onSample = viewModel.selection::sample,
                    onToggleCard = viewModel.selection::toggleCard,
                    onSelectCandidates = viewModel.selection::selectCandidates,
                    onToggle = viewModel.picker::toggle,
                    onToggleEverything = viewModel.picker::toggleEverything,
                    onToggleRanked = viewModel.picker::toggleRanked,
                    onDismiss = viewModel.picker::dismiss,
                    onStart = viewModel.picker::start,
                    onResume = viewModel.picker::resume,
                    onStartAgain = viewModel.picker::startAgain,
                    onDiscard = viewModel.notices::discardStored,
                ),
            )
        }
        DeletedItemSnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                // Over the duels, clear of Skip, which a short window puts between the cards instead.
                .padding(start = 16.dp, end = 16.dp, bottom = if (stage is PlayStage.Dueling && !isShortWindow) 96.dp else 16.dp),
        )
    }
}

/**
 * The picker, and for a moment longer once a game starts. The sheet and its
 * scrim are a window of their own; gone in the frame the game is first drawn,
 * the window can leave the screen before the game reaches it, and the bare
 * board blinks between the two. Held until the game has been drawn under it.
 */
@Composable
private fun pickerOver(stage: PlayStage): Picker? {
    var before by remember { mutableStateOf(stage) }
    val covering = stage is PlayStage.Dueling || stage is PlayStage.Finished
    LaunchedEffect(stage) {
        if (covering && before is PlayStage.Picking) repeat(FRAMES_UNDER_THE_SHEET) { withFrameNanos { } }
        before = stage
    }
    return (stage as? PlayStage.Picking)?.picker ?: (before as? PlayStage.Picking)?.picker?.takeIf { covering }
}

private const val FRAMES_UNDER_THE_SHEET = 2

private fun noticeText(notice: PlayNotice, resources: android.content.res.Resources): Pair<String, String?> = when (notice) {
    is PlayNotice.GameSaved ->
        resources.getString(R.string.play_snack_game_saved, notice.played, notice.total) to resources.getString(R.string.play_snack_discard)
    is PlayNotice.BoardArranged ->
        resources.getQuantityString(R.plurals.play_snack_arranged, notice.duels, notice.duels) to resources.getString(R.string.play_snack_undo)
    PlayNotice.SavedGameDiscarded ->
        resources.getString(R.string.play_snack_saved_discarded) to resources.getString(R.string.play_snack_undo)
    PlayNotice.ResultDiscarded ->
        resources.getString(R.string.play_snack_result_discarded) to resources.getString(R.string.play_snack_undo)
    PlayNotice.Failed -> resources.getString(ThemeR.string.snack_action_failed) to null
    PlayNotice.NotSaved -> resources.getString(R.string.play_snack_not_saved) to null
    PlayNotice.ListUpdated -> resources.getString(R.string.play_snack_list_updated) to null
}

private fun act(notice: PlayNotice, viewModel: PlayViewModel) {
    when (notice) {
        is PlayNotice.GameSaved -> viewModel.notices.discardSaved()
        is PlayNotice.BoardArranged -> viewModel.notices.undoArrange()
        PlayNotice.SavedGameDiscarded, PlayNotice.ResultDiscarded -> viewModel.notices.undoDiscard()
        PlayNotice.Failed, PlayNotice.NotSaved, PlayNotice.ListUpdated -> Unit
    }
}
