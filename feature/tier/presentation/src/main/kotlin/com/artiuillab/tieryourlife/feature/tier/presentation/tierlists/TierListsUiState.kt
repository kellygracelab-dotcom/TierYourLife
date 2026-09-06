package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardFilters
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore

sealed interface TierListsUiState {
    data object Loading : TierListsUiState

    data class Success(
        val lists: List<TierList>,
        val totalListCount: Int,
        val rankedCount: Int,
        val mode: HomeMode = HomeMode.Browsing,
        /** Your own boards drawn as pictures rather than as rows. */
        val asPictures: Boolean = false,
        /** Starred boards, already in the order they belong in. */
        val favourites: List<TierList> = emptyList(),
        /** True when the two groups are drawn apart, with a heading each. */
        val grouped: Boolean = false,
        val boardSort: BoardSort = BoardSort.Newest,
        val boardFilters: BoardFilters = BoardFilters(),
        val localOnly: LocalOnly = LocalOnly.Unknown,
        val restoringPictures: PictureRestore.Progress = PictureRestore.Progress.Idle,
        /** The board whose two versions have not been mentioned yet, if there is one. */
        val conflict: TierList? = null,
    ) : TierListsUiState

    data object Error : TierListsUiState
}

sealed interface HomeMode {
    data object Browsing : HomeMode
    data class Searching(val query: String) : HomeMode
    data class Selecting(val selectedIds: Set<Long>) : HomeMode
}

/** The two halves of the home screen. Which one is open is the home screen's business; the tab row is drawn here because its words are. */
enum class HomeTab { Mine, Community }

/**
 * [Unknown] is not [Kept]: Firebase answers a moment after the screen appears,
 * and treating them as one flashed the footer on every start.
 */
sealed interface LocalOnly {
    data object Unknown : LocalOnly

    /** Signed in. Nothing is said, because nothing is wrong. */
    data object Kept : LocalOnly

    /** A guest with boards. The footer always; the card once. */
    data class Here(val offerSignIn: Boolean) : LocalOnly
}
