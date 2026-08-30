package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.feature.tier.presentation.common.OnResumeEffect
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsViewModel

/**
 * The whole app, on a folding phone's cover screen.
 *
 * One surface and one answer. Boards are shown; everything else -- ranking,
 * adding, settings, Community -- either needs typing, needs a drag, or is a
 * decision with consequences, and none of those belong on a screen somebody
 * is glancing at with the phone still shut.
 *
 * A tap anywhere that is not a swipe between boards says so once and gets out
 * of the way, rather than a screen full of controls that all refuse.
 */
@Composable
fun CoverScreen(viewModel: TierListsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var explaining by remember { mutableStateOf(false) }
    OnResumeEffect { viewModel.loadTierLists() }

    val boards = (state as? TierListsUiState.Success)?.lists.orEmpty()

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { explaining = true }
            },
    ) {
        CoverBoard(boards)
        if (explaining) {
            UnfoldToRank(onDismiss = { explaining = false })
        }
    }
}
