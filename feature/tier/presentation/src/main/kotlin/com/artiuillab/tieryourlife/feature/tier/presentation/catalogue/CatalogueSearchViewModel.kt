package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PictureLinks
import com.artiuillab.tieryourlife.feature.tier.domain.repository.linkIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val LINK_DEBOUNCE_MILLIS = 600L
private const val MIN_QUERY_LENGTH = 2
private const val FIRST_PAGE = 1

@HiltViewModel
class CatalogueSearchViewModel @Inject constructor(
    private val repository: CatalogueSearchRepository,
    private val appPreferences: AppPreferences,
    private val links: PictureLinks,
) : ViewModel() {

    private val _state =
        MutableStateFlow<CatalogueSearchUiState>(CatalogueSearchUiState.Initial)

    val state: StateFlow<CatalogueSearchUiState> = _state.asStateFlow()

    private val _scope = MutableStateFlow(PictureScope.All)

    /** Which catalogues are asked; the board's own section until the person picks another. */
    val scope: StateFlow<PictureScope> = _scope.asStateFlow()

    private val _link = MutableStateFlow<LinkUiState?>(null)

    /** Set while the field holds a picture's address rather than words to search for. */
    val link: StateFlow<LinkUiState?> = _link.asStateFlow()

    private var linkJob: Job? = null
    private var searchJob: Job? = null
    private var moreJob: Job? = null
    private var typedQuery = ""
    private var searchedQuery = ""
    private var everywhere = false
    private var loadedPage = FIRST_PAGE
    private var started = false

    /** Once per sheet: a rotation keeps whatever scope the person chose since. */
    fun start(category: String?) {
        if (started) return
        started = true
        _scope.value = PictureScope.forCategory(category)
    }

    fun setScope(scope: PictureScope) {
        if (scope == _scope.value && !everywhere) return
        _scope.value = scope
        everywhere = false
        search(typedQuery)
    }

    /** The empty scope's way out: every catalogue and the web, for what was searched. */
    fun searchEverywhere() {
        cancelInFlight()
        everywhere = true
        _scope.value = PictureScope.All
        if (searchedQuery.length >= MIN_QUERY_LENGTH) {
            val query = searchedQuery
            searchJob = viewModelScope.launch { performSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        cancelInFlight()
        typedQuery = query
        everywhere = false
        if (followLink(query, LINK_DEBOUNCE_MILLIS)) return
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _state.value = CatalogueSearchUiState.Initial
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            performSearch(trimmed)
        }
    }

    fun search(query: String) {
        cancelInFlight()
        typedQuery = query
        if (followLink(query, waitMillis = 0)) return
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _state.value = CatalogueSearchUiState.Initial
            return
        }
        searchJob = viewModelScope.launch { performSearch(trimmed) }
    }

    /** Asks again for an address that did not come, or that needed an account somebody has now signed in to. */
    fun retryLink() {
        val shown = _link.value ?: return
        if (shown.answer is LinkPicture.Found || shown.answer == null) return
        fetchLink(shown.url, waitMillis = 0)
    }

    /** The whole field is one address: the picture behind it, not a search for its words. */
    private fun followLink(query: String, waitMillis: Long): Boolean {
        val trimmed = query.trim()
        if (linkIn(trimmed) != trimmed) {
            linkJob?.cancel()
            _link.value = null
            return false
        }
        _state.value = CatalogueSearchUiState.Initial
        if (_link.value?.url != trimmed || _link.value?.answer == null) fetchLink(trimmed, waitMillis)
        return true
    }

    private fun fetchLink(url: String, waitMillis: Long) {
        linkJob?.cancel()
        _link.value = LinkUiState(url)
        linkJob = viewModelScope.launch {
            delay(waitMillis)
            val answer = links.fetch(url)
            if (_link.value?.url == url) _link.value = LinkUiState(url, answer)
        }
    }

    fun loadMore() {
        val shown = _state.value as? CatalogueSearchUiState.Success ?: return
        if (!shown.canLoadMore || shown.loadingMore) return

        val nextPage = loadedPage + 1
        _state.value = shown.copy(loadingMore = true)
        moreJob = viewModelScope.launch {
            repository.search(searchedQuery, appPreferences.languageTag(), nextPage, _scope.value, everywhere)
                .fold(
                    onSuccess = { page -> append(nextPage, page) },
                    // A page that never arrived is no reason to take away the
                    // ones that did. The next scroll asks again.
                    onFailure = { stopWaiting() },
                )
        }
    }

    /**
     * The quick catalogues first, on screen at once; then, when the quick
     * answer says the slow ones or the web can add something, their new
     * results below, while the spinner under the grid says more is coming.
     */
    private suspend fun performSearch(trimmedQuery: String) {
        _state.value = CatalogueSearchUiState.Loading
        searchedQuery = trimmedQuery
        loadedPage = FIRST_PAGE
        val language = appPreferences.languageTag()

        val quick = repository.search(trimmedQuery, language, FIRST_PAGE, _scope.value, everywhere, quick = true)
        val first = quick.getOrNull()
        if (first == null || !first.rest) {
            show(trimmedQuery, quick)
            return
        }
        if (first.items.isNotEmpty()) show(trimmedQuery, quick, restComing = true)

        val whole = repository.search(trimmedQuery, language, FIRST_PAGE, _scope.value, everywhere)
        when {
            _state.value !is CatalogueSearchUiState.Success -> show(trimmedQuery, if (whole.isSuccess) whole else quick)
            whole.isSuccess -> append(FIRST_PAGE, whole.getOrThrow())
            else -> stopWaiting()
        }
    }

    private fun show(query: String, answer: Result<CatalogueSearchPage>, restComing: Boolean = false) {
        _state.value = answer.fold(
            onSuccess = { page ->
                if (page.items.isEmpty()) {
                    CatalogueSearchUiState.Empty(query = query, searchedEverywhere = everywhere, notice = page.notice)
                } else {
                    CatalogueSearchUiState.Success(
                        items = page.items,
                        canLoadMore = page.hasMore,
                        loadingMore = restComing,
                        notice = page.notice,
                    )
                }
            },
            onFailure = { CatalogueSearchUiState.Error },
        )
    }

    private fun append(page: Int, loaded: CatalogueSearchPage) {
        val shown = _state.value as? CatalogueSearchUiState.Success ?: return
        loadedPage = page
        val alreadyShown = shown.items.mapTo(mutableSetOf()) { it.id }
        _state.value = shown.copy(
            items = shown.items + loaded.items.filterNot { it.id in alreadyShown },
            canLoadMore = loaded.hasMore,
            loadingMore = false,
            notice = loaded.notice ?: shown.notice,
        )
    }

    private fun stopWaiting() {
        val shown = _state.value as? CatalogueSearchUiState.Success ?: return
        _state.value = shown.copy(loadingMore = false)
    }

    private fun cancelInFlight() {
        searchJob?.cancel()
        moreJob?.cancel()
    }
}
