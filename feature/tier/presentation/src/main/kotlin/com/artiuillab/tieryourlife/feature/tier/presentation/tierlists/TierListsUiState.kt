package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardFilters
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore

sealed interface TierListsUiState {
    data object Loading : TierListsUiState

    data class Success(
        val lists: List<TierListSummary>,
        val totalListCount: Int,
        val rankedCount: Int,
        val mode: HomeMode = HomeMode.Browsing,
        /** How many of them are out in the community. */
        val publicCount: Int = 0,
        /** Your own boards drawn as pictures rather than as rows. */
        val asPictures: Boolean = false,
        /** Starred boards, already in the order they belong in. */
        val favourites: List<TierListSummary> = emptyList(),
        /** True when the two groups are drawn apart, with a heading each. */
        val grouped: Boolean = false,
        val boardSort: BoardSort = BoardSort.Newest,
        val boardFilters: BoardFilters = BoardFilters(),
        /** Every section these boards are filed under, built-in or not, for the filters to offer. */
        val filedUnder: List<String> = emptyList(),
        val localOnly: LocalOnly = LocalOnly.Unknown,
        val restoringPictures: PictureRestore.Progress = PictureRestore.Progress.Idle,
        /** The board whose two versions have not been mentioned yet, if there is one. */
        val conflict: TierListSummary? = null,
        /** Other people's lists begun here and not saved yet, the last touched first. */
        val started: List<StartedList> = emptyList(),
        /** For someone with no lists yet: a popular list to try (artboard 50); gone once there is one of their own. */
        val readyList: PublishedListSummary? = null,
    ) : TierListsUiState

    data object Error : TierListsUiState
}

/** Someone else's list begun on this phone: where it stands, and whether a game of it waits. */
data class StartedList(
    val listId: String,
    val title: String,
    val placed: Int,
    val total: Int,
    val gameSaved: Boolean = false,
    /** The list's section id; null for progress kept before it was. */
    val section: String? = null,
)

sealed interface HomeMode {
    data object Browsing : HomeMode
    data class Searching(val query: String) : HomeMode
    data class Selecting(val selectedIds: Set<Long>) : HomeMode
}

/** The two halves of the home screen. Which one is open is the home screen's business; the tab row is drawn here because its words are. */
enum class HomeTab { Mine, Community }

/**
 * [Unknown] is not [Kept]: Firebase answers a moment after the screen appears,
 * and treating them as one flashed the notice on every start.
 */
sealed interface LocalOnly {
    data object Unknown : LocalOnly

    /** Signed in. Nothing is said, because nothing is wrong. */
    data object Kept : LocalOnly

    /** A guest with boards. The notice always; its sign-in buttons until answered. */
    data class Here(val offerSignIn: Boolean) : LocalOnly
}
