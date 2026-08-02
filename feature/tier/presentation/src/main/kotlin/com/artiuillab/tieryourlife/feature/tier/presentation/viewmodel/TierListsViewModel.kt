package com.artiuillab.tieryourlife.feature.tier.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.state.TierListsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class TierListsViewModel @Inject constructor(
    private val repository: TierRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TierListsUiState>(TierListsUiState.Loading)
    val state: StateFlow<TierListsUiState> = _state.asStateFlow()
    private val loadMutex = Mutex()

    init {
        loadTierLists()
    }

    fun loadTierLists() {
        viewModelScope.launch {
            loadTierListsInternal()
        }
    }

    private suspend fun loadTierListsInternal() = loadMutex.withLock {
        _state.value = TierListsUiState.Loading
        _state.value = TierListsUiState.Success(repository.loadTierListsForPresentation())
    }

    fun createTierList(title: String) {
        viewModelScope.launch {
            repository.createTierList(title)
            loadTierListsInternal()
        }
    }
}

internal suspend fun TierRepository.loadTierListsForPresentation(): List<TierList> {
    var overviewLists = getAllTierLists()
    if (overviewLists.isEmpty()) {
        createTierList("Sci-fi films")
        createTierList("Every A24 film")
        overviewLists = getAllTierLists()
    }

    return overviewLists.map { overview ->
        getTierListById(overview.id) ?: overview
    }.withDemoTierItems().withDemoListsForScroll()
}

// Temporary data for visual verification of TierLists progress bars; remove after real content is connected.
private fun List<TierList>.withDemoTierItems(): List<TierList> = map { list ->
    if (list.tiers.any { it.items.isNotEmpty() }) {
        list
    } else {
        when (list.title) {
            "Sci-fi films" -> list.withDemoTierItems(
                counts = mapOf("S" to 2, "A" to 2, "B" to 1, "C" to 1, "D" to 1),
                poolCount = 6,
                firstId = -1L,
            )
            "Every A24 film" -> list.withDemoTierItems(
                counts = mapOf("S" to 3, "A" to 3, "B" to 3, "C" to 2, "D" to 1),
                poolCount = 4,
                firstId = -101L,
            )
            else -> list
        }
    }
}

// Temporary lists for scrolling and responsive UI verification; remove after the real list flow is connected.
private fun List<TierList>.withDemoListsForScroll(totalCount: Int = 15): List<TierList> {
    val initialTitles = setOf("Sci-fi films", "Every A24 film")
    if (size != 2 || map { it.title }.toSet() != initialTitles) return this

    val tierTemplate = first().tiers
    if (tierTemplate.isEmpty()) return this

    return this + (3..totalCount).map { listNumber ->
        createScrollDemoList(
            listNumber = listNumber,
            tierTemplate = tierTemplate,
            profile = demoProfiles[(listNumber - 3) % demoProfiles.size],
        )
    }
}

private fun createScrollDemoList(
    listNumber: Int,
    tierTemplate: List<Tier>,
    profile: DemoProfile,
): TierList = TierList(
    id = -listNumber.toLong(),
    title = "Demo list ${listNumber.toString().padStart(2, '0')}",
    tiers = tierTemplate.mapIndexed { tierIndex, template ->
        val isPool = template.isPool || template.label == "Unranked"
        val itemCount = if (isPool) profile.pool else profile.counts[template.label] ?: 0
        Tier(
            id = -(listNumber * 100L + tierIndex + 1),
            label = template.label,
            colorLight = template.colorLight,
            colorDark = template.colorDark,
            items = List(itemCount) { itemIndex ->
                TierItem(
                    id = -(listNumber * 1000L + tierIndex * 100L + itemIndex + 1),
                    title = "demo_${listNumber}_${template.label}_$itemIndex",
                    imageUrl = null,
                )
            },
            isPool = isPool,
        )
    },
)

private data class DemoProfile(val counts: Map<String, Int>, val pool: Int)

private val demoProfiles = listOf(
    DemoProfile(mapOf("S" to 2, "A" to 2, "B" to 1, "C" to 1, "D" to 1), pool = 6),
    DemoProfile(mapOf("S" to 3, "A" to 3, "B" to 3, "C" to 2, "D" to 1), pool = 4),
    DemoProfile(mapOf("S" to 1, "A" to 2, "B" to 3, "C" to 2, "D" to 1), pool = 2),
    DemoProfile(mapOf("S" to 0, "A" to 1, "B" to 2, "C" to 3, "D" to 2), pool = 5),
)

private fun TierList.withDemoTierItems(
    counts: Map<String, Int>,
    poolCount: Int,
    firstId: Long,
): TierList {
    val presentationTiers = if (tiers.any { it.isPool }) {
        tiers
    } else {
        tiers.map { tier -> if (tier.label == "Unranked") tier.copy(isPool = true) else tier }
    }

    return copy(
        tiers = presentationTiers.mapIndexed { tierIndex, tier ->
            val itemCount = if (tier.isPool) poolCount else counts[tier.label] ?: 0
            if (itemCount == 0) {
                tier
            } else {
                tier.copy(
                    items = List(itemCount) { itemIndex ->
                        TierItem(
                            id = firstId - tierIndex * 20L - itemIndex,
                            title = "demo_${tier.label}_$itemIndex",
                            imageUrl = null,
                        )
                    },
                )
            }
        },
    )
}
