package com.artiuillab.tieryourlife.feature.community.presentation.allsections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionCatalog
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SectionSort { MostLists, Name }

/** Every section (docs/sections-api.md §4, "All sections"): the count, the order chosen, and a search once there are many. */
data class AllSectionsUiState(
    /** Approved sections in the server's order, before the sort and the search. */
    val sections: List<Section> = Section.BUILT_IN,
    val countsKnown: Boolean = false,
    val sort: SectionSort = SectionSort.MostLists,
    val query: String = "",
    /** This person's own proposals still waiting: first, and seen by nobody else. */
    val waiting: List<Section> = emptyList(),
    /** Their proposals declined or merged, until dismissed. */
    val notices: List<Section> = emptyList(),
) {
    /** Past twenty, scrolling for a name stops being quicker than typing it. */
    val searchable: Boolean get() = sections.size > SEARCHABLE_FROM
}

private const val SEARCHABLE_FROM = 20

@HiltViewModel
class AllSectionsViewModel @Inject constructor(
    private val catalog: SectionCatalog,
) : ViewModel() {

    private val sort = MutableStateFlow(SectionSort.MostLists)
    private val query = MutableStateFlow("")
    private val notes = Channel<CommunityNotice>(Channel.BUFFERED)

    val state: StateFlow<AllSectionsUiState> = combine(catalog.directory, sort, query) { directory, order, typed ->
        AllSectionsUiState(
            sections = directory.shown,
            countsKnown = directory.counted,
            sort = order,
            query = typed,
            waiting = directory.waiting,
            notices = directory.settledAgainst,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AllSectionsUiState())

    val notices: Flow<CommunityNotice> = notes.receiveAsFlow()

    private val sent = Channel<Unit>(Channel.CONFLATED)

    /** A proposal that arrived while this screen was open: the form just sent it. */
    val proposed: Flow<Unit> = sent.receiveAsFlow()

    init {
        viewModelScope.launch {
            var seen: Set<String>? = null
            catalog.directory.map { directory -> directory.waiting.map { it.id }.toSet() }.distinctUntilChanged().collect { waiting ->
                if (seen?.let { (waiting - it).isNotEmpty() } == true) sent.send(Unit)
                seen = waiting
            }
        }
    }

    /** Withdraws a waiting proposal, or dismisses the notice of a declined or merged one. */
    fun withdraw(sectionId: String) {
        viewModelScope.launch {
            catalog.withdraw(sectionId).onFailure { notes.send(CommunityNotice.ActionFailed) }
        }
    }

    fun open() {
        viewModelScope.launch { catalog.refresh() }
    }

    fun selectSort(order: SectionSort) {
        sort.value = order
    }

    fun updateQuery(typed: String) {
        query.value = typed
    }

    fun follow(sectionId: String) = setFollowing(sectionId, true)

    fun unfollow(sectionId: String) = setFollowing(sectionId, false)

    private fun setFollowing(sectionId: String, now: Boolean) {
        viewModelScope.launch {
            catalog.setFollowing(sectionId, now).fold(
                onSuccess = { notes.send(if (now) CommunityNotice.NowFollowing(sectionId) else CommunityNotice.Unfollowed(sectionId)) },
                onFailure = { notes.send(CommunityNotice.FollowFailed) },
            )
        }
    }
}

/**
 * The grid's order: by how many lists, or by name in the reader's language;
 * what the search names, by its label or by the name as typed. Other comes
 * last either way. Pure, so the rules are argued with in a unit test.
 */
internal fun allSectionsInOrder(
    sections: List<Section>,
    sort: SectionSort,
    query: String,
    labelOf: (Section) -> String,
    byName: Comparator<in String>,
): List<Section> {
    val wanted = query.trim()
    val found = if (wanted.isEmpty()) {
        sections
    } else {
        sections.filter { labelOf(it).contains(wanted, ignoreCase = true) || it.name.contains(wanted, ignoreCase = true) }
    }
    val ordered = when (sort) {
        SectionSort.MostLists -> found.sortedByDescending { it.listCount }
        SectionSort.Name -> found.sortedWith(compareBy(byName, labelOf))
    }
    return ordered.filterNot { it.isOther } + ordered.filter { it.isOther }
}
