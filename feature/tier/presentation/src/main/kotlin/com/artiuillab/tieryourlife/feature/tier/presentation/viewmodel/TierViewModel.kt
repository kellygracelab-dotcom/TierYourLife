package com.artiuillab.tieryourlife.feature.tier.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import com.artiuillab.tieryourlife.feature.tier.presentation.state.TierListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TierViewModel @Inject constructor(
    private val repository: TierRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tierListId = savedStateHandle.toRoute<Route.TierDetail>().tierListId
    private val _state = MutableStateFlow<TierListUiState>(TierListUiState.Loading)

    val state: StateFlow<TierListUiState> = _state

    init {
        getTierList()
    }

    fun getTierList() {
        viewModelScope.launch {
            repository.getTierListById(tierListId).let {
                if (it != null) {
                    _state.value = TierListUiState.Success(it)
                } else {
                    _state.value = TierListUiState.Error("Tier list not found")
                }
            }
        }
    }
}
