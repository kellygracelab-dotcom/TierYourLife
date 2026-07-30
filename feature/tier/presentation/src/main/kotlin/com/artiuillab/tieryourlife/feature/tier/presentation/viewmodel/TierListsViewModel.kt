package com.artiuillab.tieryourlife.feature.tier.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.state.TierListsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TierListsViewModel @Inject constructor(
    private val repository: TierRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TierListsUiState>(TierListsUiState.Loading)
    val state: StateFlow<TierListsUiState> = _state.asStateFlow()

    init {
        loadTierLists()
    }

    fun loadTierLists() {
        viewModelScope.launch {
            _state.value = TierListsUiState.Loading
            _state.value = TierListsUiState.Success(repository.getAllTierLists())
        }
    }

    fun createTierList(title: String) {
        viewModelScope.launch {
            repository.createTierList(title)
            loadTierLists()
        }
    }
}
