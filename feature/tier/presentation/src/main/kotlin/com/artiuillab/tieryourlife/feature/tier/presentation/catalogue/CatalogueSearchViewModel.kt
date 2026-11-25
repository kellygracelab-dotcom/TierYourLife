package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListKind
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchRefusal
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

/** A busy catalogue's time further off than this is not waited for: the search says it stayed busy. */
internal const val BUSY_WAIT_MAX_MILLIS = 2 * 60 * 1_000L

/** Without a time from the server, a busy catalogue is asked again after this long. */
internal const val BUSY_FALLBACK_MILLIS = 10_000L

/** How many times one search asks a busy catalogue before it gives up on it. */
internal const val BUSY_TRIES = 5

/**
 * When a busy catalogue is asked again, after the search went [tries] times and the server named [retryAt];
 * null when it is given up: asked often enough, or a time too far off. The site's rule, to the millisecond.
 */
internal fun busyWaitUntil(retryAt: Long?, tries: Int, now: Long): Long? {
    if (tries >= BUSY_TRIES) return null
    val until = retryAt ?: (now + BUSY_FALLBACK_MILLIS)
    return until.takeIf { it - now <= BUSY_WAIT_MAX_MILLIS }
}

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

    /** The words the search on its way or on screen is for, from the moment it is planned. */
    private var plannedQuery: String? = null
    private var everywhere = false
    private var loadedPage = FIRST_PAGE
    private var started = false
    private var kind = ListKind()

    /** Once per sheet: a rotation keeps whatever scope the person chose since. */
    fun start(category: String?, topic: String? = null) {
        if (started) return
        started = true
        _scope.value = PictureScope.forCategory(category)
        forList(category, topic)
    }

    /** What the list holds, sent with every search so its kind of picture comes first, whatever the scope. */
    fun forList(section: String?, topic: String?) {
        kind = ListKind(section, topic)
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
            plannedQuery = query
            searchJob = viewModelScope.launch { performSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        if (asksTheSame(query)) {
            typedQuery = query
            return
        }
        cancelInFlight()
        typedQuery = query
        everywhere = false
        if (followLink(query, LINK_DEBOUNCE_MILLIS)) return
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _state.value = CatalogueSearchUiState.Initial
            return
        }
        plannedQuery = trimmed
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            performSearch(trimmed)
        }
    }

    /**
     * A space after the words, or the keyboard settling them again, is no new
     * search: the one on its way, or its answer, is already for them. The
     * app sent "Теория" twice at once that way (Backend's logs, 24.11).
     */
    private fun asksTheSame(query: String): Boolean {
        if (query.trim() != plannedQuery) return false
        val state = _state.value
        return searchJob?.isActive == true || state is CatalogueSearchUiState.Success || state is CatalogueSearchUiState.Empty
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
        plannedQuery = trimmed
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
            repository.search(searchedQuery, appPreferences.languageTag(), nextPage, _scope.value, everywhere, kind = kind)
                .fold(
                    onSuccess = { page -> append(nextPage, page) },
                    // A page that never arrived is no reason to take away the
                    // ones that did. The next scroll asks again.
                    onFailure = { stopWaiting() },
                )
        }
    }

    /**
     * The same words until they get an answer: nothing found while a catalogue is busy is not "nothing by
     * that name", so they wait for the time the server gave and go again by themselves, as on the site.
     */
    private suspend fun performSearch(trimmedQuery: String) {
        _state.value = CatalogueSearchUiState.Loading
        searchedQuery = trimmedQuery
        loadedPage = FIRST_PAGE
        var tries = 0
        while (true) {
            tries++
            val busy = searchOnce(trimmedQuery, tries) ?: return
            val until = busyWaitUntil(busy.retryAt, tries, System.currentTimeMillis())
            if (until == null) {
                _state.value = CatalogueSearchUiState.Busy(busy.retryAt)
                return
            }
            _state.value = CatalogueSearchUiState.Waiting(until)
            delay((until - System.currentTimeMillis()).coerceAtLeast(0))
        }
    }

    /**
     * The quick catalogues first, on screen at once; then, when the quick
     * answer says the slow ones or the web can add something, their new
     * results below, while the spinner under the grid says more is coming.
     * The busy refusal when nothing came and the busy catalogue is worth waiting for.
     */
    private suspend fun searchOnce(query: String, tries: Int): SearchRefusal.Busy? {
        val language = appPreferences.languageTag()
        val quick = repository.search(query, language, FIRST_PAGE, _scope.value, everywhere, quick = true, kind = kind)
        val first = quick.getOrElse { failure ->
            if (failure is SearchRefusal) return held(failure)
            // A quick answer that would not come: the whole search, as before it was split.
            return whole(query, language, tries, quick)
        }
        if (!first.rest) {
            show(query, quick)
            refill(query, language, first, tries)
            return null
        }
        if (first.items.isNotEmpty()) show(query, quick, restComing = true)
        return whole(query, language, tries, quick)
    }

    /** The whole search, below what the quick one showed; [quick]'s answer stands when it fails with nothing shown. */
    private suspend fun whole(query: String, language: String?, tries: Int, quick: Result<CatalogueSearchPage>): SearchRefusal.Busy? {
        val whole = repository.search(query, language, FIRST_PAGE, _scope.value, everywhere, kind = kind)
        val page = whole.getOrElse { failure ->
            when {
                _state.value is CatalogueSearchUiState.Success -> stopWaiting()
                failure is SearchRefusal -> return held(failure)
                else -> show(query, if (quick.isSuccess) quick else whole)
            }
            return null
        }
        if (_state.value is CatalogueSearchUiState.Success) append(FIRST_PAGE, page) else show(query, whole)
        refill(query, language, page, tries)
        return null
    }

    /** Over the limit, the search stops and says until when; a busy catalogue is handed back to be waited for. */
    private fun held(refusal: SearchRefusal): SearchRefusal.Busy? = when (refusal) {
        is SearchRefusal.Limit -> {
            _state.value = CatalogueSearchUiState.Limited(refusal.retryAt)
            null
        }
        is SearchRefusal.Busy -> refusal
    }

    /**
     * The answer misses a busy catalogue's hits: the whole search once more when it may be asked, its new
     * hits going below the others. Nothing is said of it, and a time too far off is not waited for.
     */
    private suspend fun refill(query: String, language: String?, page: CatalogueSearchPage, tries: Int) {
        if (!page.busy) return
        val until = busyWaitUntil(page.retryAt, tries, System.currentTimeMillis()) ?: return
        delay((until - System.currentTimeMillis()).coerceAtLeast(0))
        val again = repository.search(query, language, FIRST_PAGE, _scope.value, everywhere, kind = kind).getOrNull() ?: return
        when {
            _state.value is CatalogueSearchUiState.Success -> append(FIRST_PAGE, again)
            again.items.isNotEmpty() -> show(query, Result.success(again))
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
                        searchedAs = page.searchedAs,
                    )
                }
            },
            onFailure = { CatalogueSearchUiState.Error },
        )
    }

    private fun append(page: Int, loaded: CatalogueSearchPage) {
        val shown = _state.value as? CatalogueSearchUiState.Success ?: return
        // A refill of the first page after more were loaded does not send the next page back to the second.
        loadedPage = maxOf(loadedPage, page)
        val alreadyShown = shown.items.mapTo(mutableSetOf()) { it.id }
        _state.value = shown.copy(
            items = shown.items + loaded.items.filterNot { it.id in alreadyShown },
            canLoadMore = loaded.hasMore,
            loadingMore = false,
            notice = loaded.notice ?: shown.notice,
            searchedAs = shown.searchedAs ?: loaded.searchedAs,
        )
    }

    private fun stopWaiting() {
        val shown = _state.value as? CatalogueSearchUiState.Success ?: return
        _state.value = shown.copy(loadingMore = false)
    }

    private fun cancelInFlight() {
        plannedQuery = null
        searchJob?.cancel()
        moreJob?.cancel()
    }
}
