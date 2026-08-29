package com.artiuillab.tieryourlife.feature.tier.presentation.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.ordering.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POOL_TIER_ID = -1L

@HiltViewModel
class CommunityListViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val tiers: TierRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val publishedId = savedStateHandle.toRoute<Route.CommunityList>().publishedId

    private val _state = MutableStateFlow<CommunityListUiState>(CommunityListUiState.Loading)
    val state: StateFlow<CommunityListUiState> = _state.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = CommunityListUiState.Loading
            _state.value = community.open(publishedId).fold(
                onSuccess = { published ->
                    CommunityListUiState.Success(
                        list = TierList(
                            id = 0,
                            title = published.summary.title,
                            tiers = published.tiers.map { it.copy(items = emptyList()) } + pool(published.items),
                            authorName = published.summary.authorName,
                        ),
                        authorName = published.summary.authorName,
                    )
                },
                onFailure = { CommunityListUiState.Error },
            )
        }
    }

    /**
     * Ranking happens entirely in memory. Nothing reaches the database until
     * the reader asks for a copy, so backing out costs them nothing they were
     * promised.
     */
    fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        _state.update { current ->
            if (current !is CommunityListUiState.Success) return@update current
            current.copy(
                list = current.list.withItemMoved(itemId, toTierId, toPosition),
                arranged = true,
            )
        }
    }

    fun saveToMyLists(onSaved: (Long) -> Unit) {
        val current = _state.value as? CommunityListUiState.Success ?: return
        if (current.saving) return
        _state.update { (it as CommunityListUiState.Success).copy(saving = true) }

        viewModelScope.launch {
            var newId: Long? = null
            val saved = messages.guard("Saving a community list") {
                newId = tiers.createFromTemplate(
                    title = current.list.title,
                    authorName = current.authorName,
                    tiers = current.list.tiers.filterNot { it.isPool },
                    items = current.list.tiers.flatMap { it.items },
                )
            }
            _state.update { (it as CommunityListUiState.Success).copy(saving = false) }
            if (saved) newId?.let(onSaved)
        }
    }

    private fun pool(items: List<com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem>) = Tier(
        id = POOL_TIER_ID,
        label = "Unranked",
        colorLight = "#DAD7E0",
        colorDark = "#46464F",
        items = items,
        isPool = true,
    )
}
