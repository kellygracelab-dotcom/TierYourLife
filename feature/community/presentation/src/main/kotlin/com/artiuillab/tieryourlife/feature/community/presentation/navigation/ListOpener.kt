package com.artiuillab.tieryourlife.feature.community.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * A tapped card of one's own list goes to its board when this phone has it,
 * so the list's page is never pushed only to be replaced. Everything else,
 * and their own list with no board here, opens the page.
 */
@HiltViewModel
class ListOpener @Inject constructor(private val tiers: TierRepository) : ViewModel() {

    private var looking = false

    fun open(route: CommunityRoute.CommunityList, toBoard: (Long) -> Unit, toList: (CommunityRoute.CommunityList) -> Unit) {
        if (!route.mine || route.fromBoard) return toList(route)
        if (looking) return
        looking = true
        viewModelScope.launch {
            val board = runSuspendCatching { tiers.boardPublishedAs(route.publishedId) }
                .onFailure { Timber.i(it, "Opening the page of ${route.publishedId} rather than its board") }
                .getOrNull()
            looking = false
            if (board != null) toBoard(board.id) else toList(route)
        }
    }
}

/** Where a card's tap goes: [toBoard] for one's own list whose board is here, [toList] otherwise. */
@Composable
fun rememberListOpener(
    toBoard: (Long) -> Unit,
    toList: (CommunityRoute.CommunityList) -> Unit,
    opener: ListOpener = hiltViewModel(),
): (CommunityRoute.CommunityList) -> Unit {
    val board by rememberUpdatedState(toBoard)
    val list by rememberUpdatedState(toList)
    return remember(opener) { { route -> opener.open(route, board, list) } }
}
