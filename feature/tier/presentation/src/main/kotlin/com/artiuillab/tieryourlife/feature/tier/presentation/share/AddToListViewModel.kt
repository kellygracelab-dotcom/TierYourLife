package com.artiuillab.tieryourlife.feature.tier.presentation.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PictureLinks
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.linkIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** What another app shared: a picture's file, or text that may hold an address, and the page's title. */
data class Shared(val imageUri: String? = null, val text: String? = null, val subject: String? = null) {
    val link: String? get() = if (imageUri == null) text?.let(::linkIn) else null

    val host: String? get() = link?.substringAfter("://")?.substringBefore('/')?.substringBefore('?')?.removePrefix("www.")
}

data class ListChoice(val id: Long, val title: String, val editedAt: Long?)

data class AddToListUiState(
    /** Null while the picture is being had; absent altogether when nothing shared one. */
    val picture: LinkPicture? = null,
    val hasPicture: Boolean = true,
    val lists: List<ListChoice> = emptyList(),
    /** Null for a new list. */
    val chosen: Long? = null,
    val saving: Boolean = false,
)

/**
 * The short way in from a browser: one card into a list's pool, and the
 * person back where they were to take the next one.
 */
@HiltViewModel
class AddToListViewModel @Inject constructor(
    private val repository: TierRepository,
    private val links: PictureLinks,
) : ViewModel() {

    private val _state = MutableStateFlow(AddToListUiState())
    val state: StateFlow<AddToListUiState> = _state.asStateFlow()

    private var started = false

    fun start(shared: Shared) {
        if (started) return
        started = true
        viewModelScope.launch {
            val lists = runSuspendCatching { repository.getTierListSummaries() }.getOrDefault(emptyList())
                .sortedByDescending { it.editedAt ?: 0L }
                .take(MAX_LISTS)
                .map { ListChoice(it.id, it.title, it.editedAt) }
            _state.update { it.copy(lists = lists, chosen = lists.firstOrNull()?.id) }
        }
        val link = shared.link
        when {
            shared.imageUri != null -> fetch { keep(shared.imageUri) }
            link != null -> fetch { links.fetch(link) }
            else -> _state.update { it.copy(hasPicture = false) }
        }
    }

    fun choose(listId: Long?) = _state.update { it.copy(chosen = listId) }

    /** [newListTitle] names the list made when none was chosen; [onAdded] hears which list the card went to. */
    fun add(title: String, newListTitle: String, onAdded: (String) -> Unit) {
        val now = _state.value
        if (now.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val found = now.picture as? LinkPicture.Found
            val chosen = now.lists.firstOrNull { it.id == now.chosen }
            val added = runSuspendCatching {
                val listId = chosen?.id ?: repository.createTierList(newListTitle, emptyList())
                repository.addItemsToPool(
                    listId,
                    listOf(PoolItemDraft(title = title, imageUrl = found?.imageUrl, credit = found?.credit, attribution = found?.attribution)),
                )
            }
            _state.update { it.copy(saving = false) }
            if (added.isSuccess) onAdded(chosen?.title ?: newListTitle) else Timber.w(added.exceptionOrNull(), "A shared card was not added")
        }
    }

    private fun fetch(have: suspend () -> LinkPicture) {
        viewModelScope.launch {
            val answer = have()
            _state.update { it.copy(picture = answer) }
        }
    }

    /** A file another app lent for a moment: copied now, while the loan lasts. */
    private suspend fun keep(uri: String): LinkPicture = runSuspendCatching { repository.keepPicture(uri) }
        .fold(
            onSuccess = { LinkPicture.Found(it, null, null) },
            onFailure = {
                Timber.i(it, "A shared picture could not be kept")
                LinkPicture.Refused(LinkRefusal.Failed)
            },
        )

    private companion object {
        const val MAX_LISTS = 5
    }
}
