package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.ui.OnResumeEffect
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsViewModel

/**
 * The lists the cover shows: the phone's own, read again whenever the cover
 * comes back on, since the phone may have changed them while it was open.
 */
@Composable
fun coverListsState(viewModel: TierListsViewModel = hiltViewModel()): TierListsUiState {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnResumeEffect { viewModel.loadTierLists() }
    return state
}

/** The cover's root (§4), drawn from [state]. */
@Composable
fun CoverListsPane(
    state: TierListsUiState,
    lastOpenedId: Long?,
    onOpen: (Long) -> Unit,
    onCommunity: () -> Unit,
    onHandOff: (CoverHandOffSubject) -> Unit,
) {
    CoverListsScreen(
        lists = coverOrder(state),
        lastOpenedId = lastOpenedId,
        onOpen = onOpen,
        onCommunity = onCommunity,
        onOpenMain = { onHandOff(CoverHandOffSubject.App) },
        loading = state is TierListsUiState.Loading,
    )
}

/**
 * One list open on the cover (§5). A list that is gone, deleted on the phone
 * while the cover was off, sends the cover back to the lists.
 */
@Composable
fun CoverListPane(
    state: TierListsUiState,
    listId: Long,
    onBack: () -> Unit,
    onOpenList: (Long) -> Unit,
    onHandOff: (CoverHandOffSubject) -> Unit,
) {
    val order = coverOrder(state)
    val list = order.firstOrNull { it.id == listId }
    if (list == null) {
        if (state !is TierListsUiState.Loading) LaunchedEffect(listId) { onBack() }
        return
    }
    CoverListScreen(
        list = list,
        next = nextListAfter(order, listId),
        onBack = onBack,
        onRankOnMain = { tierId -> onHandOff(CoverHandOffSubject.OwnList(list.id, list.title, tierId)) },
        onCardOnMain = { item, tierId -> onHandOff(CoverHandOffSubject.Card(list.id, tierId, item.title)) },
        onNext = onOpenList,
    )
}
