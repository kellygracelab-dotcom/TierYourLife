package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The board part of the screen, for tests that wait on the board alone. */
internal val TierDetailViewModel.board: Flow<TierDetailUiState>
    get() = screen.map { it.board }.distinctUntilChanged()
