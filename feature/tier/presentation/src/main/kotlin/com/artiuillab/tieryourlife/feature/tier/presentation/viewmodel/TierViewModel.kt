package com.artiuillab.tieryourlife.feature.tier.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.data.repository.FakeTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.state.TierListUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TierViewModel(
    private val repository: TierRepository = FakeTierRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<TierListUiState>(TierListUiState.Loading)

    val state: StateFlow<TierListUiState> = _state

    init {
        getTierList()
    }

    fun getTierList() {
        viewModelScope.launch {
            delay(1000)
            repository.getTierListById(1).let {
                if (it != null) {
                    _state.value = TierListUiState.Success(it)
                } else {
                    _state.value = TierListUiState.Error("Tier list not found")
                }
            }
        }
    }
}