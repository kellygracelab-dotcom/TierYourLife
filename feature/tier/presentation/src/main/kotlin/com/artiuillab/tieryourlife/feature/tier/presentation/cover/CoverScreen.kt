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
 * The whole app on a cover screen: boards are shown, and everything else
 * needs typing, a drag or a decision, none of which belong on a shut phone.
 * A tap on anything else says so once and gets out of the way.
 */
@Composable
fun CoverScreen(viewModel: TierListsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var explaining by remember { mutableStateOf(false) }
    OnResumeEffect {
        viewModel.loadTierLists()
        // A cover screen is closed, not dismissed: reopening should show the
        // board, not the remainder of a notice.
        explaining = false
    }

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
